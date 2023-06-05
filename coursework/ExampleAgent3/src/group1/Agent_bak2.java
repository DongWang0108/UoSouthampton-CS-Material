package group1;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import agents.anac.y2019.harddealer.math3.distribution.ChiSquaredDistribution;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import group1.EvaluatedBid;


/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent_bak2 extends AbstractNegotiationParty
{
    private static double MINIMUM_TARGET = 0.8;
    private static float MINIMUM_UTINILITY = -1;
    private Bid lastOffer;
    public int BidNumberAlready = 0;

    public List<EvaluatedBid> bidSpace;
    public List<EvaluatedBid> paretoLine = new ArrayList<EvaluatedBid>();

    public EvaluatedBid nashBid;
    public boolean spaceUpdated;
    public Bid finalnashbid;
    boolean getNash = false;

    // window size for opponent utility prediction, windowSize >= 1
    private int windowSize = 3;
    // gamma to the estimation of the valuation, 0 < gamma < 1
    private double gamma = .5;
    private int num_issues = 0;
    // alpha, beta: time computation
    /**
     * alpha: larger, faster to change weights
     * beta: larget, slower to change weights,  0 < beta < 1
     */
    private double alpha = 1.0;
    private double beta = 0.9;


    // prev window
    private HashMap<Integer, HashMap<String, Integer>> prev_window;
    // current window
    private HashMap<Integer, HashMap<String, Integer>> cur_window;
    // empty window
    private HashMap<Integer, HashMap<String, Integer>> empty_window;
    // frequency since Game Begins
    private HashMap<Integer, HashMap<String, Integer>> fr;
    // weight for every option in every Issue
    private ArrayList<Double> wi;
    private boolean wi_updated = false;

    // round
    private int game_round = 0;
    // fr_sig for modeling the Pareto Curve
    private int fr_sig = 0;
    private SortedOutcomeSpace outcomeSpace;

    private List<Bid> allBids;//拿到bid中每个issue的选择

    int chooseUserModel = 1;//选择usermodel，0-utilitySpace.getUtility ,1-模拟退火，2-遗传算法

    private UserModelSA userModelSA;
    private AbstractUtilitySpace predAbsUtilitySpace;
    private AdditiveUtilitySpace predAddUtilitySpace;

    private AbstractUtilitySpace predictAbstractSpace;
    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info)
    {

        super.init(info);//父类构造函数
        System.out.println("这是bak2");
//        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
//      Bidranking basic info print
        if (hasPreferenceUncertainty()) {
            //System.out.println("Preference uncertainty is enabled.");
            BidRanking bidRanking = userModel.getBidRanking();
            //System.out.println("The agent ID is:"+info.getAgentID());
            //System.out.println("Total number of possible bids:" +userModel.getDomain().getNumberOfPossibleBids());
            //System.out.println("The number of bids in the ranking is:" + bidRanking.getSize());
            //System.out.println("The elicitation costs are:"+user.getElicitationCost());

        }
        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();

        // Opponent Estimation init
        prev_window = new HashMap<Integer, HashMap<String, Integer>>();
        cur_window = new HashMap<Integer, HashMap<String, Integer>>();
        empty_window = new HashMap<Integer, HashMap<String, Integer>>();
        fr = new HashMap<Integer, HashMap<String, Integer>>();
        wi = new ArrayList<Double>();

        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            // Opponent Estimation init
            initOppoEsti(issueNumber, issue);
            // normalize wi
            Double sum_w = .0;
            for (double i : wi) {
                sum_w += i;
            }
            for (int i = 0; i < wi.size(); i++) {
                wi.set(i, i / sum_w);
            }
        }
        // init ends

        outcomeSpace = new SortedOutcomeSpace(utilitySpace);//获得当前domin的所有可能的输出
        allBids = outcomeSpace.getAllBidsWithoutUtilities();
        bidSpace = new ArrayList<>();

        //        User models
        long startTime = System.currentTimeMillis() ;
        if(chooseUserModel == 1){
            UserModel userModel = info.getUserModel();						//	get UserModel & initial userModelSA
            userModelSA = new UserModelSA(userModel);

            try {
                predAbsUtilitySpace = userModelSA.SimulatedAnnealing();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }		//	Predict UtilitySpace by SA
            predAddUtilitySpace = (AdditiveUtilitySpace) predAbsUtilitySpace;		// Use this interface!!!!!!!!!!!!!!!

        } else if (chooseUserModel == 2) {
            GeneticAlgorithm geneticAlgorithm=new GeneticAlgorithm(userModel);//初始化自己的模型
            predictAbstractSpace = geneticAlgorithm.geneticAlgorithm();
            predictAbstractSpace = (AdditiveUtilitySpace) predictAbstractSpace; //返回一个效用空间，这个累加效用空间，近似于当前的不确定效用空间
        }
        long endTime   = System.currentTimeMillis() ;
        long totalTime = endTime - startTime;
        //System.out.println("geneticAlgorithm "+chooseUserModel+" take time: "+ totalTime+"ms");
    }
    /**
     * User model switch.
     */
    private  Double getUserUtility(Bid b){

        if (chooseUserModel == 1){
            return predAddUtilitySpace.getUtility(b);
        } else if (chooseUserModel == 2) {
            return predictAbstractSpace.getUtility(b);
        }
        else {
            return utilitySpace.getUtility(b);
        }

    }

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

    public int getbid(){
        BidNumberAlready+= 1;
        return BidNumberAlready;
    }

    public  Bid generateNewBid3(){
        if (game_round % windowSize == 0){
            bidSpace.clear();
            for (int i=0;i<allBids.size();i++){
                bidSpace.add(new EvaluatedBid(allBids.get(i),getUserUtility(allBids.get(i)),calcOppo(allBids.get(i), windowSize, gamma)));
            }
            getNashBid3();
        }

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
                double currentutil = getUserUtility(randombid);//计算自己的utility
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
                double out_bid = getUserUtility(currentbid);
                System.out.println("myutility"+out_bid);
                //System.out.println(">>>>currentbid : "+currentbid);
                acceptablebid.clear();
                return currentbid;
            }
        }else {//利用现有的资源和john black方法中的对手权重，模拟计算若干次Bid，存进Hashmap中
            List<Bid> offerlist = new ArrayList<>();
            List<Double> distancelist = new ArrayList<>();
            for(int i = 0;i<500;i++){
                randombid = generateRandomBid();
                double myutility = getUserUtility(randombid);
                if(myutility >= 0 && myutility <= 1){
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
                double out_bid = getUserUtility(currentbid);
                System.out.println("myutility"+out_bid);
                distancelist.clear();
                offerlist.clear();
                return currentbid;

            }
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

    public double distanceToNash(Bid b){//计算距离
        double nashDistance = -1.0;
        if(nashBid != null){
            double agentUtil = nashBid.getMyutility() - getUserUtility(b);//nash点减去自己的和对方的，然后利用欧式距离计算
            double opponentUtil = nashBid.getOpponentutility() - calcOppo(b, windowSize, gamma);

            //欧式距离
            nashDistance = Math.sqrt(((Math.pow(agentUtil, 2)) + (Math.pow(opponentUtil, 2))));
        }
        return nashDistance;

    }
    public double timebase(double time){
//        Bid minbid= getMinUtility();
        Bid maxbid = getMaxUtilityBid();
        double minu = 0.6;
        double maxu = utilitySpace.getUtility(maxbid);
        double mintime = Math.min(time,1);
        double ft = Math.pow(mintime,1.9);
        double utility = minu + (1-ft)*(maxu-minu);
        System.out.println("timebase"+utility);
        return utility;
    }

    /**
     * Makes a random offer above the minimum utility target
     * Accepts everything above the reservation value at the end of the negotiation; or breaks off otherwise.
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
        Bid currentBid = generateNewBid3();
        double concession = timebase(time);
        if(lastOffer != null){
            float myOppoU = calcOppo(lastOffer, windowSize, gamma);;
            double myU = getUserUtility(lastOffer);
            System.out.println("accept_utility"+myU);
            System.out.println("concession"+concession);
            if(time<0.2){
                if(myU >= 0.97 || myU >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.5) {
                if(myU >= 0.96 || myU >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.8) {
                if(myU >= 0.95|| myU >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.9) {
                if(myU >= 0.94 || myU >= myOppoU || myU >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            } else if (time<0.98) {
                if(myU >= 0.93 || myU >= myOppoU || myU >= concession){
                    return new Accept(this.getPartyId(),lastOffer);
                }
                return new Offer(this.getPartyId(),currentBid);
            }else {
                if(myU >= 0.9){
                    return new Accept(this.getPartyId(),lastOffer);
                }
            }
        }
        //System.out.println("******current util: "+utilitySpace.getUtility(myLastOffer));
        return new Offer(this.getPartyId(), currentBid);
        //}
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
            float oppUtility = calcOppo(bid, windowSize, gamma);
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
            game_round ++;
            lastOffer = ((Offer) action).getBid();
            System.out.println(lastOffer);
            collectOppo(lastOffer);
            // if windowSize is reached, then update weights based on last two windows
            if (game_round % windowSize == 0 && game_round >= (windowSize * 2)){
                updateWeights(lastOffer);
            }
        }
    }

    @Override
    public String getDescription()
    {
        return "New opponent Model";
    }

    /**
     * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
     */
    @Override
    public AbstractUtilitySpace estimateUtilitySpace()
    {
        return super.estimateUtilitySpace();
    }

    private void initOppoEsti(int issueNumber, Issue issue){
        prev_window.put(issueNumber, new HashMap<String, Integer>(){
            {
                for (ValueDiscrete j : ((IssueDiscrete) issue).getValues()){
                    put(j.getValue(), 0);
                }
            }
        });
        cur_window.put(issueNumber, new HashMap<String, Integer>(){
            {
                for (ValueDiscrete j : ((IssueDiscrete) issue).getValues()){
                    put(j.getValue(), 0);
                }
            }
        });
        empty_window.put(issueNumber, new HashMap<String, Integer>(){
            {
                for (ValueDiscrete j : ((IssueDiscrete) issue).getValues()){
                    put(j.getValue(), 0);
                }
            }
        });
        fr.put(issueNumber, new HashMap<String, Integer>(){
            {
                for (ValueDiscrete j : ((IssueDiscrete) issue).getValues()){
                    put(j.getValue(), 0);
                }
            }
        });
        wi.add(.1);
    }

    private void updateOneWin(Bid bid, Issue issue, int issueNum, HashMap<Integer, HashMap<String, Integer>> window){
        String v = String.valueOf(bid.getValue(issue));
        HashMap<String, Integer> freqInnerMap = window.get(issueNum);
        int v_count = freqInnerMap.get(v);
        freqInnerMap.put(v, v_count+1);		// update the frequency
    }

    private void updateWindows(Bid bid, HashMap<Integer, HashMap<String, Integer>> window1, HashMap<Integer, HashMap<String, Integer>> window2){
        for(Issue issue: bid.getIssues()) {
            int issueNum = issue.getNumber();
            updateOneWin(bid, issue, issueNum, window1);
            updateOneWin(bid, issue, issueNum, window2);
        }
    }

    public static HashMap<Integer, HashMap<String, Integer>> copy(HashMap<Integer, HashMap<String, Integer>> original)
    {
        HashMap<Integer, HashMap<String, Integer>> cc = new HashMap<Integer, HashMap<String, Integer>>();
        for (Map.Entry<Integer, HashMap<String, Integer>> entry : original.entrySet()){
            cc.put(entry.getKey(), new HashMap<String, Integer>(){
                {
                    for (Entry<String, Integer> inner_ : entry.getValue().entrySet()){
                        put(inner_.getKey(), inner_.getValue());
                    }
                }
            });
        }
        return cc;
    }

    private void collectOppo(Bid bid) {
        if (game_round == 0){
            for(Issue issue: bid.getIssues()) {
                int issueNum = issue.getNumber();
                updateOneWin(bid, issue, issueNum, fr);
            }
        } else{
            if (this.fr_sig == 0){
//                fr = (HashMap<Integer, HashMap<String, Integer>>) empty_window.clone();
                fr = copy(empty_window);
                this.fr_sig = 1;
            }
            // first windowSize times, update prev_window and fr
            if (game_round <= windowSize){
                updateWindows(bid, prev_window, fr);
            }else{
                // later, only update cur_window and fr
                updateWindows(bid, cur_window, fr);
            }
        }
    }

    private Double computeV(Double v_numerator, Double v_denominator_max, Double gamma, int smoothing){
        Double result = Math.pow((1+v_numerator) / (smoothing+v_denominator_max), gamma);
        return result;
    }

    private List<Double> computeViForBid(Bid bid, HashMap<Integer, HashMap<String, Integer>> hashMap){
        // note: parameter hashMap: fr
        List<Double> Vi = new ArrayList<Double>();
        Double v_denominator_max = .0;
        for (Issue issue : bid.getIssues()){
            int issueNum = issue.getNumber();
            String v = String.valueOf(bid.getValue(issue));
            HashMap<String, Integer> freqInnerMap = hashMap.get(issueNum);

            // 分子
            Double v_count = Double.valueOf(freqInnerMap.get(v));

            // get max for issue 分母
            Double v_denominator = Double.valueOf(Collections.max(freqInnerMap.values()));
            if (v_denominator >= v_denominator_max)
                v_denominator_max = v_denominator;

            Vi.add(computeV(v_count, v_denominator_max, gamma, freqInnerMap.size()));
        }

        return Vi;
    }

    private List<Double> computeVi(Issue issue, HashMap<Integer, HashMap<String, Integer>> hashMap){
        // note: parameter hashMap: fr
        List<Double> Vi = new ArrayList<Double>();
        Vi.clear();

        Double v_denominator_max = .0;

        int issueNum = issue.getNumber();
        HashMap<String, Integer> freqInnerMap = hashMap.get(issueNum);

        // get value j for current issue 分子们
        for (Integer v_numerator : freqInnerMap.values()){
            Vi.add(Double.valueOf(v_numerator));
        }

        // get max for issue 分母
        Double v_denominator = Double.valueOf(Collections.max(freqInnerMap.values()));
        if (v_denominator >= v_denominator_max)
            v_denominator_max = v_denominator;

        for (int i=0; i<Vi.size(); i++){
            Vi.set(i, computeV(Vi.get(i), v_denominator_max, gamma, Vi.size()));
        }

//        // get max for issue 分母
//        Double v_denominator = Double.valueOf(Collections.max(freqInnerMap.values()));
//
//        for (int i=0; i<Vi.size(); i++){
//            Vi.set(i, computeV(Vi.get(i), v_denominator, gamma, Vi.size()));
//        }

        return Vi;
    }

    private Double computeFr(Double v_numerator, Double v_denominator, int smoothing){
        Double result = (1+v_numerator) / (smoothing+v_denominator);
        return result;
    }

    private List<Double> computeFriForBid(Bid bid, HashMap<Integer, HashMap<String, Integer>> hashMap){
        // note: parameter hashMap: fr
        List<Double> Vi = new ArrayList<Double>();
        Vi.clear();
        for (Issue issue : bid.getIssues()){
            int issueNum = issue.getNumber();
            String v = String.valueOf(bid.getValue(issue));
            HashMap<String, Integer> freqInnerMap = hashMap.get(issueNum);

            // 分子
            Double v_count = Double.valueOf(freqInnerMap.get(v));

            // 分母
            Double v_denominator = Double.valueOf(game_round);

            Vi.add(computeFr(v_count, v_denominator, freqInnerMap.size()));
        }

        return Vi;
    }

    private List<Double> computeFri(Issue issue, HashMap<Integer, HashMap<String, Integer>> window){
        List<Double> Fr = new ArrayList<Double>();
        List<Double> Fr_result = new ArrayList<Double>();
        Fr.clear();
        Fr_result.clear();

        int issueNum = issue.getNumber();
        HashMap<String, Integer> freqInnerMap = window.get(issueNum);

        // get value j for current issue 分子们
        for (Integer v_numerator : freqInnerMap.values()){
            Fr.add(Double.valueOf(v_numerator));
        }
        // 分母
        Double v_denominator = Double.valueOf(windowSize);

        for (int i=0; i<Fr.size(); i++){
            Fr_result.add(computeFr(Fr.get(i), v_denominator, Fr.size()));
        }

        return Fr_result;
    }

    /**
     * updateWeights: according to pseudocode in
     * Tunalı, Okan, Reyhan Aydoğan, and Victor Sanchez-Anguix. "Rethinking frequency opponent modeling in automated negotiation." International Conference on Principles and Practice of Multi-Agent Systems. Springer, Cham, 2017
     *
     * @param bid
     */
    private void updateWeights(Bid bid) {
        //System.out.println("updating weights");
        // define e and concession
        ArrayList<Integer> e = new ArrayList<Integer>();
        Boolean concession = false;
        double f_value = .0;
        Double p = .0;
        Double utility_prev = .0;
        Double utility_cur = .0;

        Double t = timeline.getTime();

        Double delta_t = alpha * (1 - Math.pow(t, beta));

        // update weights
        for (Issue issue : bid.getIssues()) {
            utility_prev = .0;
            utility_cur = .0;
            List<Double> Fri_prev = computeFri(issue, prev_window);
            List<Double> Fri_cur = computeFri(issue, cur_window);

            double chisquare_test[][] = new double[2][Fri_cur.size()];
            for (int idx=0; idx<Fri_prev.size(); idx++){
                f_value = Fri_prev.get(idx);
                chisquare_test[0][idx] = f_value;
            }
            for (int idx=0; idx<Fri_cur.size(); idx++){
                f_value = Fri_cur.get(idx);
                chisquare_test[1][idx] = f_value;
            }

            // chi-squared test
            ChiSquaredTest ct = new ChiSquaredTest(chisquare_test);
            p = ct.getPValue();
            //System.out.println("ChiSquaredTest P: "+p);

            if (p <= 0.05){
                e.add(issue.getNumber());
                //System.out.println("Issue not changed");
            }else {
                List<Double> Vi = computeVi(issue, fr);

                // compute Expected utility
                for (int i = 0; i < Vi.size(); i++) {
                    // previous
                    utility_prev += Vi.get(i) * Fri_prev.get(i);
                }
                for (int i = 0; i < Vi.size(); i++) {
                    // current
                    utility_cur += Vi.get(i) * Fri_cur.get(i);
                }

                if (utility_cur < utility_prev){
                    concession = true;
                    //System.out.println("utility_cur < utility_prev");
                    //System.out.println("utility_cur : " + utility_cur);
                    //System.out.println("utility_prev : " + utility_prev);
                } else {
                    //System.out.println("utility_cur !< utility_prev");
                    //System.out.println("utility_cur : " + utility_cur);
                    //System.out.println("utility_prev : " + utility_prev);
                }
            }
        }
        if (e.size() != num_issues && concession){
            //System.out.println("Update wi");
            //System.out.println("Before update" + wi);
            for (int i : e){
                wi.set(i-1, wi.get(i-1) + delta_t);
//                wi.set(i-1, wi.get(i-1) - t);
            }
            wi_updated = true;
            //System.out.println("After update" + wi);
        }

        // update sliding windows
        prev_window = copy(cur_window);
        cur_window = copy(empty_window);
    }


    private float calcOppo(Bid bid, int windowSize, double gamma){
        float utility = 0;
        List<Double> vi = computeViForBid(bid, fr);

        // haven't had whole vision of utility weights, use V_i
        if (wi_updated==false || game_round <= (windowSize * 2)){
            // E(U) = V_i \times F^\prime_i
            List<Double> fri = computeFriForBid(bid, fr);

            for (int i=0; i<vi.size(); i++){
                utility += vi.get(i) * fri.get(i);
            }

            return utility;
        }

        // calculate utility of opponent bid
        // normalize w
        // sum of wi
        //System.out.println("Wi normal");
        //System.out.println("wi after init: " + wi);
        Double sum_w = .0;

        for (int i=0; i<wi.size(); i++){
            sum_w += wi.get(i);
        }

        for (int i=0; i<vi.size(); i++){
            utility += vi.get(i) * (wi.get(i) / sum_w);
        }

        //System.out.println("final unility: " + Float.toString(utility));
        return utility;
    }
}
