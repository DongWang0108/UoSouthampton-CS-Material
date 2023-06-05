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

import java.util.*;

public class GeneticAlgorithm {
    private UserModel userModel;
    private List<AbstractUtilitySpace> group =new ArrayList<AbstractUtilitySpace>();
    //iteration paras
    private int groupSize =500;
    private int round = 4;
    private int roundNum =100;
    //select paras
    private int chooseBatch = 400;
    private double selectRemain = 2;
    //cross paras
    double variationRate = 0.1;
    private double mutationRate=0.04;
    private Random random=new Random();

    public GeneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
    }

    public AbstractUtilitySpace geneticAlgorithm(){

        for(int i = 0; i< groupSize * round; i++){
            group.add(randomInit());
        }
        System.out.println("population.size(): "+ group.size());
        for(int num = 0; num < roundNum; num++){
            List<Double> fitnessList=new ArrayList<>();

            for(int i=0; i < group.size(); i++){
                fitnessList.add(getFitness(group.get(i)));
            }

            group =selectNext(group,fitnessList, groupSize);
//            System.out.println("population.size(): "+population.size());

            for(int i=0; i < groupSize*variationRate; i++){
                AbstractUtilitySpace child=crossover();
                group.add(child);
            }
//            System.out.println("group.size():"+group.size());


        }

        double maxFit = -1;
        AbstractUtilitySpace ret = null;
        for(AbstractUtilitySpace i: group){
            double tmp = getFitness(i);
            if(tmp > maxFit){
                maxFit = tmp;
                ret = i;
            }
        }
        System.out.println("Final fit: "+ maxFit);
        return  ret;
    }
    //    Fitness equals to loss func
    private double getFitness(AbstractUtilitySpace abstractUtilitySpace){
        BidRanking bidRanking = userModel.getBidRanking();   //1.先从userModel中取出bidRanking列表
        List<Bid> choseBids =new ArrayList<>();
        int bidS = bidRanking.getSize();
        if(bidS > chooseBatch){
            HashMap<Integer, Bid> bidR = new HashMap<Integer, Bid>();
            Integer count = 0;
            List<Integer> countL= new ArrayList<>();
            for(Bid bid: bidRanking){
                bidR.put(count, bid);
                countL.add(count);
                count += 1;
            }
            Collections.shuffle(countL);
            List<Integer> subL = countL.subList(0, chooseBatch);
            Collections.sort(subL);
            for(Integer c: subL){
                choseBids.add(bidR.get(c));
            }
        }else {
            for(Bid bid: bidRanking){
                choseBids.add(bid);
            }
        }

        TreeMap<Integer,Double> uRank=new TreeMap<>();

        for(int i=0; i<choseBids.size(); i++){
            Bid bid = choseBids.get(i);
            double newU = abstractUtilitySpace.getUtility(bid);
            uRank.put(i, newU);
        }
        Comparator<Map.Entry<Integer,Double>> valueComp = Comparator.comparingDouble(Map.Entry::getValue);
        List<Map.Entry<Integer,Double>> lRank = new ArrayList<>(uRank.entrySet());
        Collections.sort(lRank, valueComp);

        int spearmanDistance=0;
        for(int i=0;i<lRank.size();i++){
            int dis = Math.abs(lRank.get(i).getKey()-i);
            spearmanDistance += dis*dis;
        }
        int N = lRank.size();
        double p = spearmanDistance/(Math.pow(N, 3));
        double score = 1 - p;
//        System.out.println("Error:"+spearmanDistance);
        return score;

    }

    private AbstractUtilitySpace randomInit(){		// generate a random Utility Space to begin annealing
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<Issue> issues = new ArrayList<Issue>(additiveUtilitySpaceFactory.getDomain().getIssues());
        for(Issue issue:issues){
            additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble()+1e-2);	// +0.01 to prevent too small
            //System.out.print("Generate weight = " + additiveUtilitySpaceFactory.getUtilitySpace().getWeight(issue) + "; ");
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            List<Value> values = new ArrayList<Value>(issueDiscrete.getValues());
            List<Double> randEvals = new ArrayList<Double>();
            double sum = 0;
            for (int i = 0; i < values.size(); i++) {
                double r = random.nextDouble()+1e-2;
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

    private List<AbstractUtilitySpace> selectNext(List<AbstractUtilitySpace> population, List<Double> fitList, int batchSize){

        List<AbstractUtilitySpace> chosenNext = new ArrayList<>();
        List <Double> fitNew= new ArrayList<>();
        for(int i=0;i<fitList.size();i++){
            fitNew.add(fitList.get(i));
        }
        Collections.sort(fitNew, Collections.reverseOrder());
//        System.out.println(fitNew);
//        double sumU = 0;
        for(int i=0;i<selectRemain;i++){
            double top = fitNew.get(i);
            int id = fitList.indexOf(top);
            chosenNext.add(population.get(id));
        }
        double sumFit=0.0;
        for(int i=0; i<selectRemain; i++){
            sumFit+=fitList.get(i);
        }

        double sumU = fitList.stream().reduce((double) 0, (a, b)->a+b);
        int N = population.size();
        for (int i=0; i < (batchSize-selectRemain); i++){
            double r = random.nextDouble()*(sumFit);
            double probAccum = 0;
            for(int j=0; j<N; j++){
                probAccum += fitList.get(j);
//                System.out.println(probAccum+" "+r);
                if(probAccum > r){
                    chosenNext.add(population.get(j));
                    break;
                }
            }
        }
        return chosenNext;
    }
    private AbstractUtilitySpace crossover(){
        AdditiveUtilitySpace parent1 = (AdditiveUtilitySpace) group.get(random.nextInt(groupSize));
        AdditiveUtilitySpace parent2 = (AdditiveUtilitySpace) group.get(random.nextInt(groupSize));

        double mutDepth=random.nextDouble();

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issuesList=additiveUtilitySpaceFactory.getIssues();
        for(IssueDiscrete i:issuesList){
            double wgtsParent1 = parent1.getWeight(i);
            double wgtsParent2 = parent2.getWeight(i);
            // crossover
            double  r = random.nextDouble();
            double wIssueChild =0;
            double wMean = (wgtsParent1 + wgtsParent2)/2;
            if(r>0.5){
                wIssueChild = wMean*(1-mutDepth) + wgtsParent1*mutDepth;
            }else{
                wIssueChild = wMean*(1-mutDepth) + wgtsParent2*mutDepth;
            }
            additiveUtilitySpaceFactory.setWeight(i,wIssueChild);

            //mutation
            if(random.nextDouble()<mutationRate) additiveUtilitySpaceFactory.setWeight(i,random.nextDouble());

            for(ValueDiscrete v:i.getValues()){
                wgtsParent1=((EvaluatorDiscrete)parent1.getEvaluator(i)).getDoubleValue(v);
                wgtsParent2=((EvaluatorDiscrete)parent2.getEvaluator(i)).getDoubleValue(v);
                // crossover
                double  r2 = random.nextDouble();
                double wValueChild =0;
                double valueMean = (wgtsParent1 + wgtsParent2)/2;
                if(r2>0.5){
                    wValueChild = valueMean*(1-mutDepth) + wgtsParent1*mutDepth;
                }else{
                    wValueChild = valueMean*(1-mutDepth) + wgtsParent2*mutDepth;
                }
                additiveUtilitySpaceFactory.setUtility(i, v, wValueChild);
                //mutation
                if(random.nextDouble()<mutationRate) additiveUtilitySpaceFactory.setUtility(i,v,random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }

}