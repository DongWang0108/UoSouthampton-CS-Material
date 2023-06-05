package group1;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.sql.Timestamp;
import java.util.*;
/*
 * 	lab 4: different to course, using Simulated annealing
 *  Class UserModelSA: main class of method
 */
public class UserModelSA {
    private UserModel userModel;
    private Random rand = new Random();				// in order to generate random number

    // Adjustable parameters of SA
    // Do not forget adjust MSE parameters!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private final double initT = 1e0;		// initial temperature, fixed to 1
    private double divInitTendT = 1e16;		// this div with delta can limit time to about 1 min
    private double endT = initT / divInitTendT;	// end temperature
    private double delta = 0.9998;			// iteration factor
    private int numPerGroup = 50;			// split bidranking into several groups
    private double alpha = -8;				// e^(α*△U*√(-lgT))
    private double beta = 0.05*Math.pow(numPerGroup, 0.5);	// △U < beta, then change a group, this expression is related to how we calculate MSE
    private double gamma = 0.02;			// max vibration scale = 0.98*e^(-γ*t), get t from seedTimes

    private BidRanking bidRanking;
    HashMap<Integer, List<Bid>> bidRankGroups;

    public UserModelSA(UserModel userModel_) {		// constructor
        userModel = userModel_;
    }
    int itersPerCheck = 10000;
    public AbstractUtilitySpace SimulatedAnnealing() throws Exception {
        double T = initT;		// current temperature

        int groups;				// deal with several seeds at the same time
        List<AdditiveUtilitySpace> tempResults = new ArrayList<AdditiveUtilitySpace>();
        List<Double> prevUtilDiff = new ArrayList<Double>();
        // List<Double> currUtility = new ArrayList<Double>();

        // get bidRank from UserModel and split it into part(s)
        bidRanking = userModel.getBidRanking();
        int num = bidRanking.getSize();
        if(num <= 0) {							// error
            System.out.println("Error: no bidRanking");
            return null;
        }
        // split bidRanking into groups and store
        groups = (num-1)/numPerGroup + 1;
        bidRankGroups = new HashMap<Integer, List<Bid>>();
        List<Integer> groupIndexs = new ArrayList<Integer>();		// corresponding bidorder group's number to each seed
        List<Integer> seedRounds = new ArrayList<Integer>();		// how many rounds does one seed comparing with one group
        List<Integer> seedTimes = new ArrayList<Integer>();			// how long (millisecond) does one seed comparing with one group
        if(groups == 1) {
            bidRankGroups.put(0, bidRanking.getBidOrder());
            numPerGroup = num;
        }
        else {
            for(int i = 0; i < groups; i++) {				// step length = num of groups
                List<Bid> bidRank = new ArrayList<Bid>();
                for(int j = 0; j < numPerGroup; j = j + groups) {
                    int index = Math.min(i+j, num-1);		// index cannot larger than num-1
                    bidRank.add(bidRanking.getBidOrder().get(index));
                }
                bidRankGroups.put(i, bidRank);
            }
        }
        for(int i = 0; i < groups; i++) {			// generate several different seeds & initial Recording Lists
            AdditiveUtilitySpace tmp = new AdditiveUtilitySpace((AdditiveUtilitySpace)genRandUtilSpace());
            tempResults.add(tmp);
            groupIndexs.add(i);
            seedRounds.add(0);
            seedTimes.add(0);
            prevUtilDiff.add(Double.MAX_VALUE);	// initial distance is double max, so that it can be changed at first iteration
        }
        int iter = 0;
        long whileBeginTime = System.currentTimeMillis();
        while(T > endT) {
            long iterBeginTime = System.currentTimeMillis();
            for(int i = 0; i < groups; i++) {
                long beginTime = System.currentTimeMillis();
                boolean updated = false;
                AdditiveUtilitySpace tempResult = tempResults.get(i);
                if(seedRounds.get(i) == 0) {
                    seedTimes.set(i, 0);
                    prevUtilDiff.set(i, calDistance(tempResult, groupIndexs.get(i), iter));
                }

                AdditiveUtilitySpace currResult = vibrateWeight(tempResult, seedTimes.get(i), iter);
                double currUtilDiff = calDistance(currResult, groupIndexs.get(i), iter);
                double deltaU = (currUtilDiff - prevUtilDiff.get(i)) / currUtilDiff;
                double ex = alpha*deltaU*Math.pow((-Math.log10(T)), 0.5);
                double p = Math.exp(ex);
                if(deltaU < 0)
                    updated = true;
                else {
                    double randDouble = rand.nextDouble();
                    if(randDouble < p)
                        updated = true;
                }
                if(iter % itersPerCheck == 0) {
                    System.out.printf("currUtilDiff = %.5f, prevUtilDiff = %.5f ",currUtilDiff, prevUtilDiff.get(i));
                    System.out.printf("deltaU = %.10f, changing possibility = %.10f\n", deltaU, p);
                }
                if(updated) {			// change tempResult & corresponding utility difference
                    tempResults.set(i, currResult);
                    prevUtilDiff.set(i, currUtilDiff);
                }
                if(currUtilDiff < beta && updated) {		// change training set
                    groupIndexs.set(i, (groupIndexs.get(i)+1)%groups);
                    seedRounds.set(i, 0);
                }
                else {			// record total time & rounds this seed using this bidGroup as training set
                    seedRounds.set(i, seedRounds.get(i)+1);
                    long endTime = System.currentTimeMillis();
                    seedTimes.set(i, seedTimes.get(i)+(int)(endTime-beginTime));
                }
            }
            T *= delta;		// update T
            iter++;
            long iterTotalTime = System.currentTimeMillis()-iterBeginTime;
            if(System.currentTimeMillis()-whileBeginTime+iterTotalTime > 59000)	// prevent timeout
                break;
        }
        System.out.println();
        long whileEndTime = System.currentTimeMillis();
        System.out.println("num of iters = " + iter + ", total time = " + (whileEndTime-whileBeginTime) + "ms");
        // Select the tempResult which got lowest UtilDiff (MSE)
        int minIndex = 0;
        for(int i = 1; i < prevUtilDiff.size(); i++) {
            if(prevUtilDiff.get(minIndex)> prevUtilDiff.get(i))
                minIndex = i;
        }
        // System.out.println("minIndex = " + minIndex + " prevUtilDiff size = " + prevUtilDiff.size() + " tmpResult size = " + tempResults.size());
        AdditiveUtilitySpace finalResult = tempResults.get(minIndex);

        return finalResult;
    }

    private AbstractUtilitySpace genRandUtilSpace(){		// generate a random Utility Space to begin annealing
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<Issue> issues = new ArrayList<Issue>(additiveUtilitySpaceFactory.getDomain().getIssues());
        for(Issue issue:issues){
            additiveUtilitySpaceFactory.setWeight(issue, rand.nextDouble()+1e-2);	// +0.01 to prevent too small
            //System.out.print("Generate weight = " + additiveUtilitySpaceFactory.getUtilitySpace().getWeight(issue) + "; ");
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            List<Value> values = new ArrayList<Value>(issueDiscrete.getValues());
            List<Double> randEvals = new ArrayList<Double>();
            double sum = 0;
            for (int i = 0; i < values.size(); i++) {
                double r = rand.nextDouble()+1e-2;
                randEvals.add(r);
                sum += r;
            }
            for (int i =0; i < values.size(); i++) {
                additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)(values.get(i)),randEvals.get(i)/sum);
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();

        return  additiveUtilitySpaceFactory.getUtilitySpace();
    }

    private AdditiveUtilitySpace vibrateWeight(AdditiveUtilitySpace oldUtilSpace, int seedTime, int iter) throws Exception {		// vibrate values of weights every iteration
        AdditiveUtilitySpace utilSpace = new AdditiveUtilitySpace(oldUtilSpace);
        List<Issue> issues = utilSpace.getDomain().getIssues();
        List<Double> issueWeights = new ArrayList<Double>();
        // record issue weight
        for(Issue issue:issues) {
            issueWeights.add(utilSpace.getWeight(issue));
        }
        // vibrate issue weight
        // vibrate only one (random) issue and one of its evaluations now! Others do scaling
        int r = rand.nextInt(issues.size());
        double randProportion1 = 0.98*(2*rand.nextDouble()-1)*Math.exp(-gamma*(seedTime)/1000);	// scale descends while time goes
        double prevWeight = issueWeights.get(r);
        double newWeight;
        if(randProportion1>0)						// new weight cannot out of the limitation
            newWeight = Math.min(prevWeight*(1+randProportion1), 0.98);
        else
            newWeight = Math.max(prevWeight*(1+randProportion1), 0.02);
        for(int i = 0; i < issues.size(); i++) {
            double prev = issueWeights.get(i);
            Issue issue = issues.get(i);
            if(i!=r) {
                issueWeights.set(i, prev*(1-newWeight)/(1-prevWeight));
            }
            else {
                issueWeights.set(i, newWeight);

                // similar method when vibrate a evaluation of the selected issue
                EvaluatorDiscrete evalDiscrete = (EvaluatorDiscrete)utilSpace.getEvaluator(issue.getNumber());
                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
                List<Value> values = new ArrayList<Value>(issueDiscrete.getValues());
                List<Double> evalsOfOneIssue = new ArrayList<Double>();
                double evalDivMaxSum = 0;
                for (Value value:values){
                    ValueDiscrete valueDiscrete = (ValueDiscrete)value;
                    double evalDivMax = evalDiscrete.getEvaluation(valueDiscrete);
                    evalsOfOneIssue.add(evalDivMax);		// when get evaluation, we get eval/maxEval,
                    // but then normalize these evaluations such that their addition is 1.
                    evalDivMaxSum += evalDivMax;
                }
                if(evalDivMaxSum==0)
                    System.out.println("Error: evaluation sum = 0");
                for(int j = 0; j < evalsOfOneIssue.size(); j++)		// the sum of this list turn to 1;
                    evalsOfOneIssue.set(j, evalsOfOneIssue.get(j)/evalDivMaxSum);

                int ra = rand.nextInt(values.size());
                double randProportion2 = 0.98*(2*rand.nextDouble()-1)*Math.exp(-gamma*(seedTime)/1000);
                double prevEval = evalsOfOneIssue.get(ra);
                double newEval;
                if(randProportion2>0)
                    newEval = Math.min(prevEval*(1+randProportion2), 0.98);
                else
                    newEval = Math.max(prevEval*(1+randProportion2), 0.02);
                for(int j = 0; j < values.size(); j++) {
                    double prev2 = evalsOfOneIssue.get(j);
                    Value value = values.get(j);
                    ValueDiscrete valueDiscrete = (ValueDiscrete)value;
                    if(j!=ra)
                        evalsOfOneIssue.set(j, prev2*(1-newEval)/(1-prevEval));
                    else
                        evalsOfOneIssue.set(j, newEval);
                    //System.out.print("	value:" + valueDiscrete.getValue() + " eval before reset: " + prev2);
                    if(evalsOfOneIssue.get(j)<0) {		// Error check
                        System.out.println("Error: negative eval: " + evalsOfOneIssue.get(j) + " ra=" + ra + " j=" + j+ " newEval=" + newEval + " prevEval=" + prevEval);
                    }
                    evalDiscrete.setEvaluationDouble(valueDiscrete, evalsOfOneIssue.get(j));	// we can guarantee the sum of evaluations is 1
                }
            }

            boolean unlock = utilSpace.unlock(issue);
            if(!unlock) {
                System.out.println("Error: Unlock failed!");
            }

            utilSpace.setWeight(issue, issueWeights.get(i));
            boolean lock = utilSpace.lock(issue);
            if(!lock) {
                System.out.println("Error: lock failed!");
            }
        }

        return utilSpace;

    }



    // calculate the distance between given preference order & current simulation
    private double calDistance(AdditiveUtilitySpace utilSpace, int groupIndex, int iter) throws Exception {
        // find original bidRankGroup, then construct correspondence between bid and index/simulatedUtility
        List<Bid> bidRankGroup = new ArrayList<Bid>(bidRankGroups.get(groupIndex));
        List<Double> simulUtils = new ArrayList<Double>();
        int numOfGroup = bidRankGroup.size();
        for(int i = 0; i < numOfGroup; i++)
            simulUtils.add(calSimulUtil(utilSpace, bidRankGroup.get(i)));
        TreeMap<Integer, Double> simulUtilsRank = new TreeMap<Integer, Double>();	// sort the ranking by TreeMap
        for(int i = 0; i < simulUtils.size(); i++)
            simulUtilsRank.put(i, simulUtils.get(i));
        Comparator<Map.Entry<Integer, Double>> MyComparator = Comparator.comparingDouble(Map.Entry::getValue);
        List<Map.Entry<Integer, Double>> newBidRank = new ArrayList<>(simulUtilsRank.entrySet());
        Collections.sort(newBidRank, MyComparator);

        // calculate MSE
        double MSE = 0.0;
        for(int i = 0; i < numOfGroup; i++) {
            //if(iter%check==0 && i < 5)
            //System.out.print(i+","+newBidRank.get(i).getKey()+", value= "+newBidRank.get(i).getValue()+"  ");
            double diff = Math.abs(i-newBidRank.get(i).getKey());
            MSE += diff*diff;
        }
        //if(iter % check == 0)
        //System.out.printf("\n iter = %d, MSE = %.10f, size = %d, simulUtils[0] = %.10f\n", iter, MSE, numOfGroup, simulUtils.get(0));

        MSE = MSE/(Math.pow(bidRankGroup.size(), 1));	// more processes such that: lower MSE --> better performance
        //MSE = Math.log(MSE+1e-5);					// parameters need adjustment!!!
        MSE = Math.pow(MSE, 0.5);
        return MSE;
    }

    private double calSimulUtil(AdditiveUtilitySpace utilSpace, Bid b) throws Exception {
        double result = 0.0;
        List<Issue> issues = b.getIssues();
        for(Issue issueOfBid:issues){
            Issue issueOfUtilSpace = utilSpace.getDomain().getIssues().get(issueOfBid.getNumber()-1);
            if(issueOfUtilSpace == null) {
                System.out.println("Error: Simulation Utility Space do not find issue No."+issueOfBid.getNumber());
                return 0.0;
            }
            double weight = utilSpace.getWeight(issueOfUtilSpace);
            EvaluatorDiscrete evalDiscrete = (EvaluatorDiscrete)utilSpace.getEvaluator(issueOfUtilSpace.getNumber());
            ValueDiscrete valueOfBid = (ValueDiscrete)b.getValue(issueOfBid);
            Set<ValueDiscrete> valuesOfUtilSpace = evalDiscrete.getValues();
            boolean findValue = false;
            for(ValueDiscrete value: valuesOfUtilSpace) {
                if(value.equals(valueOfBid)) {
                    //double eval = evalDiscrete.getDoubleValue(value);
                    double eval = evalDiscrete.getEvaluation(value);
                    result += weight * eval;
                    findValue = true;
                    break;
                }
            }
            if(!findValue) {		            	// shouldn't arrive at this part
                System.out.println("Error: Simulation Utility Space do not find value: "+valueOfBid);
                return 0.0;
            }
        }
        return result;
    }

}