package group1;

import java.sql.Timestamp;
import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;



/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent1 extends AbstractNegotiationParty
{
    private static double MINIMUM_TARGET = 0.8;
    private static float MINIMUM_UTINILITY = -1;
    private Bid lastOffer;
    public int BidNumberAlready = 0;
    HashMap<Issue, List<ValueNew>> oppo = new HashMap<Issue, List<ValueNew>>();


    private UserModelSA userModelSA;
    private AbstractUtilitySpace predAbsUtilitySpace;
    private AdditiveUtilitySpace predAddUtilitySpace;

    public List<EvaluatedBid> bidSpace;
    public List<EvaluatedBid> paretoLine = new ArrayList<EvaluatedBid>();

    public EvaluatedBid nashBid;
    public boolean spaceUpdated;
    public Bid finalnashbid;
    boolean getNash = false;
    public int n;
    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info)
    {

        super.init(info);//父类构造函数
        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();


        SortedOutcomeSpace outcomeSpace = new SortedOutcomeSpace(utilitySpace);//获得当前domin的所有可能的输出
        List<Bid> allBids = outcomeSpace.getAllBidsWithoutUtilities();//拿到bid中每个issue的选择
        System.out.println("bids"+allBids.size());
        bidSpace = new ArrayList<>();
        for (int i=0;i<allBids.size();i++){
            collectOppo(allBids.get(i));
            bidSpace.add(new EvaluatedBid(allBids.get(i),utilitySpace.getUtility(allBids.get(i)),calcOppo(allBids.get(i))));
            //System.out.println("oppo1"+calcOppo(allBids.get(i)));

        }
        getNashBid3();
        double time = getTimeLine().getTime();
        System.out.println("time"+time);

//        n = rand.nextInt();
//        if (n == 9){
//
//        }

//        for (EvaluatedBid evaluatedBid: bidSpace) {
//            addBidToPareto(evaluatedBid);
//        }
//        for (EvaluatedBid evaluatedBid: bidSpace) {
//            generatePareto(evaluatedBid);
//        }

        //finalnashbid = getNashBid2();

        //System.out.println("finalnashbid"+finalnashbid);
        //System.out.println("nashbidgetbid"+nashBid.getBid());

//        for (EvaluatedBid evaluatedBid: bidSpace) {
//            System.out.println("without"+evaluatedBid.opponentutility);
//        }

//        user models
        if (hasPreferenceUncertainty()) {
            System.out.println("Preference uncertainty is enabled.");
        } else {
            System.out.println("Preference uncertainty is unenabled!!!!");
        }
//        UserModel userModel = info.getUserModel();						//	get UserModel & initial userModelSA
//        userModelSA = new UserModelSA(userModel);
//
//        predAbsUtilitySpace = userModelSA.SimulatedAnnealing();		//	Predict UtilitySpace by SA
//        predAddUtilitySpace = (AdditiveUtilitySpace) predAbsUtilitySpace;

//        for (Issue issue : issues) {
//            int issueNumber = issue.getNumber();
//            //System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));
//
//            // Assuming that issues are discrete only
//            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
//            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);
//
//            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
//                //System.out.println(valueDiscrete.getValue());
//                //System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
//                try {
//                    //System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }



    }

    public double timebase(double time){
        Bid minbid= getMinUtility();
        Bid maxbid = getMaxUtilityBid();
        double minu = 0.6;
        double maxu = utilitySpace.getUtility(maxbid);
        double mintime = Math.min(time,1);
        double ft = Math.pow(mintime,1.9);
        double utility = minu + (1-ft)*(maxu-minu);
        System.out.println("time"+utility);
        return utility;
    }

    private  Bid getMinUtility(){
        try{
            Bid bid = this.utilitySpace.getMinUtilityBid();
            return bid;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Bid getMaxUtilityBid() {
        try {
            return utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Makes a random offer above the minimum utility target
     * Accepts everything above the reservation value at the end of the negotiation; or breaks off otherwise.
     */
//    @Override
    public Action chooseAction_bak(List<Class<? extends Action>> possibleActions)
    {
        // Check for acceptance if we have received an offer
        if (lastOffer == null){
            return new Offer(getPartyId(), generateRandomBidAboveTarget());
        }
        if (timeline.getTime() >= 0.9)//Accessing normalized time t∈[0,1]，double time = getTimeLine().getTime()
            if (getUtility(lastOffer) >= utilitySpace.getReservationValue())
                return new Accept(getPartyId(), lastOffer);
            else
                return new EndNegotiation(getPartyId());

        // Otherwise, send out a random offer above the target utility
        return new Offer(getPartyId(), maxOppo(generateRandomBidListAboveTarget()));
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
        Bid currentBid = generateNewBid3();
        double concession = timebase(time);

        if(lastOffer != null){
            float myOppoU = calcOppo(lastOffer);
            double myU = utilitySpace.getUtility(lastOffer);
            //System.out.println("----------------------------");
            //System.out.println("time: "+Double.toString(time)+" myU: "+Double.toString(myU) +" myOppoU: "+Float.toString(myOppoU));
            if(time<0.2){
                if(utilitySpace.getUtility(lastOffer) >= concession){
//                if(myU >= 0.3 || myU >= myOppoU){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.5) {
                if(utilitySpace.getUtility(lastOffer) >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.8) {
                if(utilitySpace.getUtility(lastOffer) >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.9) {
                if(myU >= myOppoU || utilitySpace.getUtility(lastOffer)>= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.98) {
                if(myU >= myOppoU || utilitySpace.getUtility(lastOffer)>= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            }else {
                if(utilitySpace.getUtility(lastOffer)>= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
            }
        }

        //System.out.println("******current util: "+utilitySpace.getUtility(myLastOffer));
        return new Offer(this.getPartyId(), currentBid);
        //}

    }
//    @Override
//    public Action chooseAction(List<Class<? extends Action>> list) {
//        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
//        Bid currentBid = generateNewBid3();
//        double concession = timebase(time);
//
//        if(lastOffer != null){
//            float myOppoU = calcOppo(lastOffer);
//            double myU = utilitySpace.getUtility(lastOffer);
//            //System.out.println("----------------------------");
//            //System.out.println("time: "+Double.toString(time)+" myU: "+Double.toString(myU) +" myOppoU: "+Float.toString(myOppoU));
//            if(time<0.2){
//                if(myU >= 0.88 || utilitySpace.getUtility(lastOffer) >= concession){
////                if(myU >= 0.3 || myU >= myOppoU){
//                    return new Accept(this.getPartyId(),lastOffer);
//                }
//                return new Offer(this.getPartyId(),currentBid);
//            } else if (time<0.5) {
//                if(myU >= 0.83 || utilitySpace.getUtility(lastOffer) >= concession){
//                    return new Accept(this.getPartyId(),lastOffer);
//                }
//                return new Offer(this.getPartyId(),currentBid);
//            } else if (time<0.8) {
//                if(myU >= 0.8 || utilitySpace.getUtility(lastOffer) >= concession){
//                    return new Accept(this.getPartyId(),lastOffer);
//                }
//                return new Offer(this.getPartyId(),currentBid);
//            } else if (time<0.9) {
//                if(myU >= 0.78 || myU >= myOppoU || utilitySpace.getUtility(lastOffer)>= concession){
//                    return new Accept(this.getPartyId(),lastOffer);
//                }
//                return new Offer(this.getPartyId(),currentBid);
//            } else if (time<0.98) {
//                if(myU >= 0.75 || myU >= myOppoU || utilitySpace.getUtility(lastOffer)>= concession){
//                    return new Accept(this.getPartyId(),lastOffer);
//                }
//                return new Offer(this.getPartyId(),currentBid);
//            }else {
//                if(myU >= 0.6){
//                    return new Accept(this.getPartyId(),lastOffer);
//                }
//            }
//        }
//
//        //System.out.println("******current util: "+utilitySpace.getUtility(myLastOffer));
//        return new Offer(this.getPartyId(), currentBid);
//        //}
//
//    }




    public int getbid(){
        BidNumberAlready+= 1;
        return BidNumberAlready;
    }
    public  Bid generateNewBid2(){
        Bid currentbid = null;//当前Bid
        Bid randombid = null;
        HashMap<Integer,Bid> acceptablebid = new HashMap<>();//存储对手的bid（模拟数据）
        List<Double> nashlist = new ArrayList<>();
        double time = getTimeLine().getTime();
        double concession = 0.0f;
        int bidalready = getbid();
        if (time<0.06){//当时间小于0.06时，我只输出最大的utility，欺骗对手，同时为john black争取数据（对手的bid）
            currentbid = getMaxUtilityBid();
            return currentbid;
        }else if (time >= 0.05 && time < 0.5){//当时间在这个阈值时，设置自己的concession
            concession = (double) (0.005*bidalready);//concession 按照每次0.006更新迭代
            double targetutil = (double) (1 - concession);//计算concession之后的utility
            List<Bid> bidlist = new ArrayList<Bid>();
            List<Double> utilitylist = new ArrayList<>();
            for(int i = 0; i<=2000;i++){
                randombid = generateRandomBid();
                double currentutil = utilitySpace.getUtility(randombid);//计算自己的utility
                bidlist.add(randombid);
                utilitylist.add(currentutil);
                if (currentutil>= (targetutil-0.1) && currentutil <= targetutil){//只要生成的bid的我的utility在我的可接受范围内，就加入到hashmap中
                    acceptablebid.put(0,randombid);
                    //System.out.println(">>>>>>>>>currentutil : "+currentutil);
                    break;
                }
            }
            if(acceptablebid.isEmpty()){//如果还是没找到，就直接给一个最大utility bid
                double utility = Collections.max(utilitylist);
                Integer index = utilitylist.indexOf(utility);
                currentbid = bidlist.get(index);
                utilitylist.clear();
                bidlist.clear();
                return currentbid;
            }else {//
                currentbid = acceptablebid.get(0);
                //System.out.println(">>>>currentbid : "+currentbid);
                acceptablebid.clear();
                return currentbid;
            }
        }else {//利用现有的资源和john black方法中的对手权重，模拟计算若干次Bid，存进Hashmap中
            List<Bid> offerlist = new ArrayList<>();
            for(int i = 0;i<2500;i++){
                randombid = generateRandomBid();
                double currentutil = utilitySpace.getUtility(randombid);
                if(currentutil >= 0.7 && currentutil <= 0.85){
                    double opponentutility = calcOppo(randombid);
                    double nashproduct = Calutilityproduct(currentutil,opponentutility);
                    nashlist.add(nashproduct);
                    offerlist.add(randombid);
                }
            }
            if(offerlist.isEmpty()){
                randombid = generateRandomBid();
                return randombid;
            }else {
                double maxnashproduct = Collections.max(nashlist);
                int index = nashlist.indexOf(maxnashproduct);
                currentbid = offerlist.get(index);
                nashlist.clear();
                offerlist.clear();
                return currentbid;

            }
        }
    }

    public  Bid generateNewBid3(){
        Bid currentbid = null;//当前Bid
        Bid randombid = null;
        HashMap<Integer,Bid> acceptablebid = new HashMap<>();//存储对手的bid（模拟数据）
        List<Double> nashlist = new ArrayList<>();
        double time = getTimeLine().getTime();
        double concession = 0.0f;
        int bidalready = getbid();
        if (time<0.06){//当时间小于0.06时，我只输出最大的utility，欺骗对手，同时为john black争取数据（对手的bid）
            currentbid = getMaxUtilityBid();
            return currentbid;
        }else if (time >= 0.05 && time < 0.5){//当时间在这个阈值时，设置自己的concession
            concession = (double) (0.004*bidalready);//concession 按照每次0.006更新迭代
            double targetutil = (double) (1 - concession);//计算concession之后的utility
            List<Bid> bidlist = new ArrayList<Bid>();
            List<Double> utilitylist = new ArrayList<>();
            for(int i = 0; i<=2000;i++){
                randombid = generateRandomBid();
                double currentutil = utilitySpace.getUtility(randombid);//计算自己的utility
                bidlist.add(randombid);
                utilitylist.add(currentutil);
                if (currentutil>= (targetutil-0.1) && currentutil <= targetutil){//只要生成的bid的我的utility在我的可接受范围内，就加入到hashmap中
                    acceptablebid.put(0,randombid);
                    //System.out.println(">>>>>>>>>currentutil : "+currentutil);
                    break;
                }
            }
            if(acceptablebid.isEmpty()){//如果还是没找到，就直接给一个最大utility bid
                double utility = Collections.max(utilitylist);
                Integer index = utilitylist.indexOf(utility);
                currentbid = bidlist.get(index);
                utilitylist.clear();
                bidlist.clear();
                return currentbid;
            }else {//
                currentbid = acceptablebid.get(0);
                //System.out.println(">>>>currentbid : "+currentbid);
                acceptablebid.clear();
                return currentbid;
            }
        }else {//利用现有的资源和john black方法中的对手权重，模拟计算若干次Bid，存进Hashmap中
            List<Bid> offerlist = new ArrayList<>();
            List<Double> distancelist = new ArrayList<>();
            for(int i = 0;i<1000;i++){
                randombid = generateRandomBid();
                double myutility = utilitySpace.getUtility(randombid);
                if(myutility >= 0.8 && myutility <= 0.88){
                    offerlist.add(randombid);
                    double distance = distanceToNash(randombid);
                    //System.out.println("distance"+distance);

                    distancelist.add(distance);
                }


            }
            if(offerlist.isEmpty()){
                randombid = generateRandomBid();
                return randombid;
            }else {
                double distance = Collections.min(distancelist);
                //System.out.println("distance"+distance);
                int index = distancelist.indexOf(distance);
                currentbid = offerlist.get(index);
                distancelist.clear();
                offerlist.clear();
                return currentbid;

            }
        }
    }





    public double Calutilityproduct(double myutility , double opponentutility){
        return myutility*opponentutility;
    }

    private Bid generateRandomBidAboveTarget()
    {
        Bid randomBid;
        double util;
        int i = 0;
        // try 100 times to find a bid under the target utility
        do
        {
            randomBid = generateRandomBid();
            util = utilitySpace.getUtility(randomBid);
        }
        while (util < MINIMUM_TARGET && i++ < 100);
        return randomBid;
    }

    private List<Bid> generateRandomBidListAboveTarget()
    {
        List<Bid> bids = new ArrayList<>();
        double util;
        int i = 0;
        Bid randomBid;
        // try 100 times to find a bid under the target utility
        do
        {
            randomBid = generateRandomBid();
            util = utilitySpace.getUtility(randomBid);
            if(util > MINIMUM_TARGET){
                bids.add(randomBid);
            }
        }
        while ( i++ < 1000);
        System.out.println("randomBids_len:" + Integer.toString(bids.size()));
        return bids;
    }

    private Bid maxOppo(List<Bid> bids){
        Bid maxBid = null;
        float maxUtility =  MINIMUM_UTINILITY;
        for(Bid bid: bids){
            float oppUtility = calcOppo(bid);
            if(oppUtility > maxUtility){
                maxBid = bid;
                maxUtility = oppUtility;
            }
        }
        return maxBid;
    }

    /**
     * Remembers the offers received by the opponent.
     */
    @Override
    public void receiveMessage(AgentID sender, Action action)
    {
        if (action instanceof Offer)
        {
            lastOffer = ((Offer) action).getBid();
            //System.out.println(lastOffer);
            collectOppo(lastOffer);



        }
    }

    @Override
    public String getDescription()
    {
        return "Places random bids >= " + MINIMUM_TARGET;
    }

    /**
     * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
     */
    @Override
    public AbstractUtilitySpace estimateUtilitySpace()
    {
        return super.estimateUtilitySpace();
    }


    private void collectOppo(Bid bid) {

        for(Issue issue: bid.getIssues()) {
            int issueNum = issue.getNumber();
            Value bidValue = bid.getValue(issueNum);
            if(oppo.get(issue) == null) {
                List<ValueNew> nn = new ArrayList<>();
                ValueNew v = new ValueNew(bidValue);
                v.count += 1;
                nn.add(v);
                oppo.put(issue, nn);
            } else {
                boolean flag = true;
                for(ValueNew v: oppo.get(issue)){
                    if(bidValue.equals(v.valueName)){
                        v.count += 1;
                        flag = false;
                    }
                }
                if(flag){
                    ValueNew v = new ValueNew(bidValue);
                    v.count += 1;
                    oppo.get(issue).add(v);
                }
            }
            assert oppo.get(issue) != null;
            oppo.get(issue).sort(oppo.get(issue).get(0));
            //System.out.println(oppo.get(issue));
        }
        //System.out.println(oppo.toString());
    }

    private float calcOppo(Bid lastOffer){
        float utility = 0;
        List<Issue> issues = lastOffer.getIssues();

        for(Issue issue: issues){
//            System.out.println("----------------------------");
 //           System.out.println(issue);
            List<ValueNew> opts = oppo.get(issue);
//            System.out.println("opts"+opts);
            Value value = lastOffer.getValue(issue);
//            System.out.println(value);
            int rank = 0;
            int countBids = 0;
            float issueWgt = 0;
            for(ValueNew v: opts){
                issueWgt += (float) (v.count * v.count);
                countBids += v.count;
            }
            issueWgt =  issueWgt / (countBids * countBids);
            for(ValueNew v:opts){
                if(v.valueName.equals(value)){
                    float optWgt = ((float) opts.size() - rank)/opts.size();
//                    System.out.println("optWgt: " + Float.toString(optWgt));
                    utility += issueWgt * optWgt;
                    break;
                }

                rank +=1;
            }
//            System.out.println("issueWgt: " + Float.toString(issueWgt));
//            System.out.println("utility: " + Float.toString(utility));
        }
//        System.out.println("final unility: " + Float.toString(utility));
        return utility;
    }

    /*
     * 	lab 4: different to course, using Simulated annealing
     *  Class UserModelSA: main class of method
     */
    public class UserModelSA {
        private UserModel userModel;
        private Random rand = new Random();				// in order to generate random number

        // Adjustable parameters of SA
        // Do not forget adjust MSE parameters!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        private double initT = 100.0;			// initial temperature
        private double endT = 1e-8;				// end temperature
        private double delta = 0.98;			// iteration factor
        private int numPerGroup = 200;			// split bidranking into several groups
        private double alpha = 1;				// e^(-α*△U*T)
        private double beta = 1e-2;				// △U < beta, then change a group
        private double gamma1 = 1, gamma2 = 1;	// max vibration scale = e^(-γ*t), get t from seedTimes
        //private double timeLimit = 0;			// Time Limit, set according to competition rules

        private BidRanking bidRanking;
        HashMap<Integer, List<Bid>> bidRankGroups;

        public UserModelSA(UserModel userModel_) {		// constructor
            userModel = userModel_;
        }

        public AbstractUtilitySpace SimulatedAnnealing() {
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());	// TimeStamp

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
            List<Long> seedTimes = new ArrayList<Long>();				// how long does one seed comparing with one group
            if(groups == 1) {
                bidRankGroups.put(0, bidRanking.getBidOrder());
                numPerGroup = num;
                groupIndexs.add(0);
                seedRounds.add(0);
            }
            else {
                for(int i = 0; i < groups; i++) {				// step length = groups
                    List<Bid> bidRank = new ArrayList<Bid>();
                    for(int j = 0; j < numPerGroup; j = j + groups) {
                        int index = Math.min(i+j, num-1);		// index cannot larger than num-1
                        bidRank.add(bidRanking.getBidOrder().get(index));
                    }
                    bidRankGroups.put(i, bidRank);
                    groupIndexs.add(i);
                    seedRounds.add(0);
                }
            }

            for(int i = 0; i < groups; i++) {			// generate several different seeds
                tempResults.add((AdditiveUtilitySpace)genRandUtilSpace());
            }
            while(T > endT) {
                for(int i = 0; i < groups; i++) {
                    long beginTime = timeStamp.getTime();
                    boolean updated = false;
                    AdditiveUtilitySpace tempResult = tempResults.get(i);
                    if(seedRounds.get(i) == 0) {
                        seedTimes.add((long)0);
                        prevUtilDiff.add(calDistance(tempResult, groupIndexs.get(i)));
                    }

                    AdditiveUtilitySpace currResult = vibrateWeight(tempResult);
                    double currUtilDiff = calDistance(currResult, groupIndexs.get(i));
                    double deltaU = currUtilDiff - prevUtilDiff.get(i);
                    if(deltaU < 0)
                        updated = true;
                    else {
                        double p = Math.exp((-1)*alpha*deltaU/T);
                        double randDouble = rand.nextDouble();
                        if(randDouble < p)
                            updated = true;
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
                        long endTime = timeStamp.getTime();
                        seedTimes.set(i, seedTimes.get(i)+endTime-beginTime);
                    }
                }
                T *= delta;		// update T
            }

            // Select the tempResult which got lowest UtilDiff (MSE)
            int minIndex = 0;
            for(int i = 1; i < prevUtilDiff.size(); i++) {
                if(prevUtilDiff.get(minIndex)> prevUtilDiff.get(i))
                    minIndex = i;
            }
            AdditiveUtilitySpace finalResult = tempResults.get(minIndex);

            return finalResult;
        }

        private AbstractUtilitySpace genRandUtilSpace(){		// generate a random Utility Space to begin annealing
            AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
            List<Issue> issues = additiveUtilitySpaceFactory.getDomain().getIssues();
            for(Issue issue:issues){
                additiveUtilitySpaceFactory.setWeight(issue, rand.nextDouble());
                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
                for (Value value:issueDiscrete.getValues()){
                    additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)value,rand.nextDouble());
                }
            }
            additiveUtilitySpaceFactory.normalizeWeights();
            return  additiveUtilitySpaceFactory.getUtilitySpace();
        }

        private AdditiveUtilitySpace vibrateWeight(AdditiveUtilitySpace oldUtilSpace) {		// vibrate values of weights every iteration
            AdditiveUtilitySpace utilSpace = new AdditiveUtilitySpace(oldUtilSpace);
            List<Issue> issues = utilSpace.getDomain().getIssues();
            for(Issue issue:issues){
                // vibrate weight
                double weight = utilSpace.getWeight(issue);
                double randProportion = (2*rand.nextDouble()-1)*gamma1;		// the value of gamma1 need adjustment
                utilSpace.setWeight(issue, weight*(1+randProportion));

                // vibrate evaluation
                EvaluatorDiscrete evalDiscrete = (EvaluatorDiscrete)utilSpace.getEvaluator(issue.getNumber());
                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
                for (Value value:issueDiscrete.getValues()){
                    ValueDiscrete valueDiscrete = (ValueDiscrete)value;
                    double evaluation = evalDiscrete.getDoubleValue(valueDiscrete);
                    randProportion = (2*rand.nextDouble()-1)*gamma2;			// the value of gamma2 need adjustment
                    evalDiscrete.setEvaluationDouble(valueDiscrete, evaluation*(1+randProportion));
                }
            }
            utilSpace.normalizeWeights();		// weight normalization, evaluation need not
            return utilSpace;

            // Later I'll try CosineAnnealing in this function.
        }

        // calculate the distance between given preference order & current simulation
        // now calculate (index distance)^2
        private double calDistance(AdditiveUtilitySpace utilSpace, int groupIndex) {
            // find original bidRankGroup, then construct correspondence between bid and index
            List<Bid> bidRankGroup = new ArrayList<Bid>(bidRankGroups.get(groupIndex));
            HashMap<Bid, Integer> originBidIndex = new HashMap<Bid, Integer>();
            int numOfGroup = bidRankGroup.size();
            for(int i = 0; i < numOfGroup; i++)
                originBidIndex.put(bidRankGroup.get(i), i);

            // calculate simulation utility for each bid，then reorder bids in ascending order
            Collections.sort(bidRankGroup, new Comparator<Bid>() {		// sort List<Option> in descending order
                @Override
                public int compare(Bid b1, Bid b2) {
                    return calSimulUtil(utilSpace, b1) < calSimulUtil(utilSpace, b2) ? -1 : 1;
                }
            });

            // calculate MSE
            double MSE = 0;
            for(int i = 0; i < bidRankGroup.size(); i++) {
                double diff = Math.abs(i-originBidIndex.get(bidRankGroup.get(i)));
                MSE += diff*diff;
            }
            MSE = MSE/(Math.pow(bidRankGroup.size(), 3));	// more processes such that: lower MSE --> better performance
            MSE = 15*Math.log(MSE+1e-5);					// parameters need adjustment!!!
            return MSE;
        }

        private double calSimulUtil(AdditiveUtilitySpace utilSpace, Bid b) {
            double result = 0.0;
            List<Issue> issues = b.getIssues();
            for(Issue issueOfBid:issues){
                Issue issueOfUtilSpace = utilSpace.getDomain().getIssues().get(issueOfBid.getNumber());
                if(issueOfUtilSpace == null) {
                    System.out.println("Error: Simulation Utility Space do not find issue No."+issueOfBid.getNumber());
                    return 0.0;
                }
                double weight = utilSpace.getWeight(issueOfUtilSpace);

                EvaluatorDiscrete evalDiscrete = (EvaluatorDiscrete)utilSpace.getEvaluator(issueOfUtilSpace.getNumber());
                ValueDiscrete valueOfBid = (ValueDiscrete)b.getValue(issueOfBid);
                Set<ValueDiscrete> valuesOfUtilSpace = evalDiscrete.getValues();
                for(ValueDiscrete value: valuesOfUtilSpace) {
                    if(value.equals(valueOfBid)) {
                        double eval = evalDiscrete.getDoubleValue(value);
                        result += weight * eval;
                        break;
                    }
                    // shouldn't arrive at this part
                    System.out.println("Error: Simulation Utility Space do not find value");
                    return 0.0;
                }
            }
            return result;
        }



    }

    /**
     * 计算nash距离
     */
    //将所有bid存到空间中
    //取出该utilityspace中的所有可能的bid，利用自己的模型进行计算



//    public void initBidSpace(List<Bid> allBids){
//        bidSpace = new ArrayList<EvaluatedBid>();
//        for(int i=0;i<allBids.size();i++){
//            collectOppo(allBids.get(i));
//            bidSpace.add(new EvaluatedBid(allBids.get(i),utilitySpace.getUtility(allBids.get(i)),calcOppo(allBids.get(i))));
//            //System.out.println("oppo1"+calcOppo(allBids.get(i)));
//        }
//        spaceUpdated = true;
//    }
//    //比较pareto better off


    public void generatePareto(EvaluatedBid evaluatedBid){
        List<EvaluatedBid> removelist = new ArrayList<>();
        if (paretoLine.isEmpty()){
            paretoLine.add(evaluatedBid);
        }else {
            for (int i = 0;i<paretoLine.size();i++){
                if(paretoLine.get(i) != evaluatedBid){
                    //pareto中我的utility比目标我的utility大
                    if(paretoLine.get(i).getMyutility() > evaluatedBid.getMyutility()){
                    //pareto中对手utility也大于目标的对手utility
                        if(paretoLine.get(i).getOpponentutility() > evaluatedBid.getOpponentutility()){
                            return;
                        } else if (paretoLine.get(i).getOpponentutility() < evaluatedBid.getOpponentutility()) {
                            continue;
                        }
                        //pareto的我的utility比目标的我的utility小
                    } else if (paretoLine.get(i).getMyutility() < evaluatedBid.getMyutility()){
                        //pareto的对手utility比目标的丢手utility小
                        if (paretoLine.get(i).getOpponentutility() < evaluatedBid.getOpponentutility()){
                            removelist.add(paretoLine.get(i));
                        } else if (paretoLine.get(i).getOpponentutility() > evaluatedBid.getOpponentutility()) {
                            continue;
                        }
                    }
                }
            }
            paretoLine.add(evaluatedBid);//不是就renturn了，能到这的一定是pareto
        }
        paretoLine.removeAll(removelist);
        removelist.clear();
    }

//    public boolean betterBid(EvaluatedBid b1, EvaluatedBid b2){
//        boolean betterBid = false;
//        if (b2 != b1) {
//            if ((b2.getMyutility() < b1.getMyutility()) || (b2.getOpponentutility() < b1.getOpponentutility())) {
//                // One of the utilities is smaller
//                betterBid = false;
//            } else if ((b2.getMyutility() > b1.getMyutility()) || (b2.getOpponentutility() > b1.getOpponentutility())) {
//                // At least one utility is strictly greater
//                betterBid = true;
//            }
//        }
//        return betterBid;
//    }

    //移除worse的
    //pareto list存的是allbids的index，通过index找allbids中对应的bid

//    private void removeWorseBid(EvaluatedBid bidToAdd, EvaluatedBid bidToRemove){//移除差的bid
//        List<EvaluatedBid> bidsToRemove = new ArrayList<EvaluatedBid>();
//        paretoLine.remove(bidToRemove);
//
//        for (int i = 0; i < paretoLine.size(); i++) {
//            if(betterBid(paretoLine.get(i), bidToAdd)){
//                bidsToRemove.add(paretoLine.get(i));
//            }
//        }
//        paretoLine.removeAll(bidsToRemove);
//        paretoLine.add(bidToAdd);
//    }

    //添加到pareto曲线上的list中


//    private void addBidToPareto(EvaluatedBid evaluatedBid){
//        for (int i = 0; i < paretoLine.size(); i++) {//判断是否better off，帕累托定理
//            // if there exists a bid that makes a agent better off
//            if(betterBid(evaluatedBid, paretoLine.get(i))){
//                return;//跳出函数,证明当前bid没paretoline中的大
//            }
//            // replace bid that might no longer be on the line
//            if(betterBid(paretoLine.get(i),evaluatedBid)){//替换现有bid，如果目前这个更好
//                removeWorseBid(evaluatedBid,paretoLine.get(i));
//            }
//        }
//        paretoLine.add(evaluatedBid);
//    }
    //计算nashpoint



//    public Bid getNashBid(){
//        if (spaceUpdated){//当bidspace更新后才能使用，因为nash有可能不被找到，对pareto中的所有点都计算，找到最大的就是nashpoint
//            for (int i = 0; i < bidSpace.size(); i++) {
//                addBidToPareto(bidSpace.get(i));
//            }
//            double max = -1.0;
//            double product = 0;
//            for (int i = 0; i < paretoLine.size(); i++) {
//                product = paretoLine.get(i).getMyutility() * paretoLine.get(i).getOpponentutility();
//                if (product > max) {
//                    nashBid = paretoLine.get(i);
//                    max = product;
//                }
//            }
//        }
//        return nashBid == null ? null : nashBid.getBid();
//    }

//    public Bid getNashBid2(){
//        for (int i = 0; i < bidSpace.size(); i++) {
//            generatePareto(bidSpace.get(i));
//        }
//        double max = -1.0;
//        double product = 0;
//        for (int i = 0; i < paretoLine.size(); i++) {
//            product = paretoLine.get(i).getMyutility() * paretoLine.get(i).getOpponentutility();
//            if (product > max) {
//                nashBid = paretoLine.get(i);
//                max = product;
//            }
//        }
//        return nashBid == null ? null : nashBid.getBid();
//    }

    public void getNashBid3(){
        for (int i = 0; i < bidSpace.size(); i++) {
            generatePareto(bidSpace.get(i));
        }
        double max = -1;
        double product = 0;
        for (int i = 0; i < paretoLine.size(); i++) {
            product = paretoLine.get(i).getMyutility() * paretoLine.get(i).getOpponentutility();
            if (product > max) {
                nashBid = paretoLine.get(i);
                max = product;
                getNash = true;
            }
        }
        System.out.println(getNash);
    }




    public double distanceToNash(Bid b){//计算距离
        double nashDistance = -1.0;
        if(nashBid != null){
            double agentUtil = nashBid.getMyutility() - utilitySpace.getUtility(b);//nash点减去自己的和对方的，然后利用欧式距离计算
            double opponentUtil = nashBid.getOpponentutility() - calcOppo(b);

            //欧式距离
            nashDistance = Math.sqrt(((Math.pow(agentUtil, 2)) + (Math.pow(opponentUtil, 2))));
        }
        return nashDistance;

    }





}