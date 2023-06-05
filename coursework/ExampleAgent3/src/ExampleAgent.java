import java.sql.Timestamp;
import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.analysis.BidPoint;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * ExampleAgent returns the bid that maximizes its own utility for half of the negotiation session.
 * In the second half, it offers a random bid. It only accepts the bid on the table in this phase,
 * if the utility of the bid is higher than Example Agent's last bid.
 */
public class ExampleAgent extends AbstractNegotiationParty {
    private final String description = "Example Agent";


    public ValueWeight valueWeight;
    public HashMap<Issue,List<ValueWeight>> issueweight = new HashMap<Issue,List<ValueWeight>>(); //存放issue中的每个option
    public HashMap<Issue,Double> weightList = new HashMap<Issue,Double>(); //存放issue权重
    //private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;

    //private IaMap iaMap;
    public int BidNumberAlready = 0;
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        //this.iaMap = new IaMap(this.userModel);

        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace)utilitySpace;
        //List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
        //issueweight = new HashMap<Issue,List<ValueWeight>>();
        for(Issue issue : userModel.getDomain().getIssues()){//将domain下的若干issue遍历
            IssueDiscrete values = (IssueDiscrete) issue;//issuediscrete 生成issue类，values中存的是该issue下的所有value
            List<ValueWeight> list = new ArrayList<>();//用于将稍后遍历的没有一个value的权重存在list中
            for (int i = 0;i<values.getNumberOfValues();i++){//遍历values
                ValueWeight valueWeight = new ValueWeight(values.getValue(i));
                list.add(valueWeight);
            }
            issueweight.put(issue,list);//将issue和issue下的所有value的权重存在Iamap中
        }
//        List<AbstractUtilitySpace> additiveUtilitySpaceFactoryList = new ArrayList<AbstractUtilitySpace>();
//        for(int i = 0;i<10;i++){
//            additiveUtilitySpaceFactoryList.add(getRandomspace());
//        }
//        for (AbstractUtilitySpace i:additiveUtilitySpaceFactoryList) {
//            //System.out.println("abstractutility"+i);
//        }


//        Iterator<Map.Entry<Issue, List<ValueWeight>>> iterator = issueweight.entrySet().iterator();
//        while (iterator.hasNext()){
//            Map.Entry<Issue,List<ValueWeight>> entry = iterator.next();
//            for (ValueWeight vallueweight: entry.getValue()
//            ) {
//                System.out.println("getvalue: "+vallueweight);
//            }
//            System.out.println("getkey: "+entry.getKey());
//        }

    }



    /**
     * When this function is called, it is expected that the Party chooses one of the actions from the possible
     * action list and returns an instance of the chosen action.
     *
     * @param list
     * @return
     */
//    @Override
//    public Action chooseAction(List<Class<? extends Action>> list) {
//        // According to Stacked Alternating Offers Protocol list includes
//        // Accept, Offer and EndNegotiation actions only.
//        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
//        // The time is normalized, so agents need not be
//        // concerned with the actual internal clock.
//
//        // First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
//        int BidAlready = getbid();
//        System.out.println("BidAlready :" + BidAlready);
//        Bid bidMax = getMaxUtilityBid();
//        Bid bidMin = getMinUtility();
//        double Maxutil = utilitySpace.getUtility(bidMax) * (double) (BidAlready* 0.01);
//        double Minutil = utilitySpace.getUtility(bidMin);
//        double currentutil = (double) (Maxutil - Minutil)/2;
//        double yourutil = 0.0f;
//        Bid currentBid = getanother(currentutil);
//        System.out.println(">>>>lastreceiveoffer : "+myLastOffer);
//            // Accepts the bid on the table in this phase,
//            // if the utility of the bid is higher than Example Agent's last bid.
//        if (myLastOffer != null
//                && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUtility(myLastOffer)) {
//
//            return new Accept(this.getPartyId(), lastReceivedOffer);
//        } else {
//            myLastOffer = bidMax;
//            return new Offer(this.getPartyId(), myLastOffer);
//        }
//
//    }
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
        double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
        // The time is normalized, so agents need not be
        // concerned with the actual internal clock.

        // First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
        //Bid currentBid = generateNewBid2();
        Bid currentBid2 = generateNewBid3();
        //System.out.println("size"+receivelistBid.size()+"utility"+receivelistBid.get(0));

        // Accepts the bid on the table in this phase,
        // if the utility of the bid is higher than Example Agent's last bid.
//        if (myLastOffer != null) {
//
//            return new Accept(this.getPartyId(), myLastOffer);
//        } else {

//        double tifU = tif(myLastOffer,currentBid2);
//        System.out.println("tif"+tifU);

        double concession = timebase(time);
        /*
        * 随时间降低自己的utilit接受程度
        *
        * */
        if(myLastOffer != null){
            if(time<0.2){
                if(utilitySpace.getUtility(myLastOffer)>= 0.8 || utilitySpace.getUtility(myLastOffer)>= concession){
                    return new Accept(this.getPartyId(),myLastOffer);
                }
                return new Offer(this.getPartyId(),currentBid2);
            } else if (time<0.5) {
                if(utilitySpace.getUtility(myLastOffer) >= 0.72 || utilitySpace.getUtility(myLastOffer)>= concession){
                    return new Accept(this.getPartyId(),myLastOffer);
                }
                return new Offer(this.getPartyId(),currentBid2);
            } else if (time<0.8) {
                if(utilitySpace.getUtility(myLastOffer) >= 0.7 || utilitySpace.getUtility(myLastOffer)>= concession){
                    return new Accept(this.getPartyId(),myLastOffer);
                }
                return new Offer(this.getPartyId(),currentBid2);
            } else if (time<0.9) {
                if(utilitySpace.getUtility(myLastOffer) >= 0.65 || utilitySpace.getUtility(myLastOffer)>= concession || utilitySpace.getUtility(myLastOffer) >= calUtil(myLastOffer)){
                    return new Accept(this.getPartyId(),myLastOffer);
                }
                return new Offer(this.getPartyId(),currentBid2);
            } else if (time<0.98) {
                if(utilitySpace.getUtility(myLastOffer) >= 0.63 || utilitySpace.getUtility(myLastOffer)>= concession || utilitySpace.getUtility(myLastOffer) >= calUtil(myLastOffer)){
                    return new Accept(this.getPartyId(),myLastOffer);
                }
                return new Offer(this.getPartyId(),currentBid2);
            }else {

                if(utilitySpace.getUtility(myLastOffer) >= 0.4){

                    return new Accept(this.getPartyId(),myLastOffer);
                }
            }
        }

        //System.out.println("******current util: "+utilitySpace.getUtility(myLastOffer));
        return new Offer(this.getPartyId(), currentBid2);
        //}

    }

    public  double tif(Bid myLastOffer,Bid currentBid){
        double mylasU = utilitySpace.getUtility(currentBid);

        if(myLastOffer != null){
            double oppolasU = utilitySpace.getUtility(myLastOffer);
            System.out.println("myutil"+oppolasU);
            double concession = mylasU - oppolasU;
            return concession;
        }else {
            return 0.99;
        }


    }

    public double timebase(double time){
        Bid minbid= getMinUtility();
        Bid maxbid = getMaxUtilityBid();
        double minu = 0.6;
        double maxu = utilitySpace.getUtility(maxbid);
        double mintime = Math.min(time,1);
        double ft = Math.pow(mintime,1.9);
        double utility = minu + (1-ft)*(maxu-minu);
        return utility;
    }





    @Override
    public void receiveMessage(AgentID sender, Action act) {
        super.receiveMessage(sender, act);

        if (act instanceof Offer) { // sender is making an offer
            myLastOffer = ((Offer) act).getBid();
            //iaMap.JohnBlack(myLastOffer);
            JohnBlack(myLastOffer);


        }
    }

    /**
     * A human-readable description for this party.
     * @return
     */
    @Override
    public String getDescription() {
        return description;
    }






    public void JohnBlack(Bid myLastOffer){
        BidNumberAlready += 1;//当一个bid开启时，bidnumber＋1
        //System.out.println("BidNumberAlready"+BidNumberAlready);
        for (Issue issue : myLastOffer.getIssues()){//lastoffer.getIssues()得到domain中的每一个issue
            int currentNum = issue.getNumber();//当前issue的num用于后面匹配issue中的value的名字

            for (ValueWeight valueWeight : issueweight.get(issue)){//遍历每一个issue中的option
                if(valueWeight.valueName.toString().equals(myLastOffer.getValue(currentNum).toString())){//名字匹配时，该option的计数加一
                    valueWeight.count += 1;

                }
                //System.out.println(valueWeight +"   valuecount"+valueWeight.count);

                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;//初始化issue
                valueWeight.k = issueDiscrete.getNumberOfValues();//issue中共有多少个options
                valueWeight.BidAlready = this.BidNumberAlready;//将当前的bid赋值给option中
            }


            Collections.sort(issueweight.get(issue),issueweight.get(issue).get(0));//利用valueweight中的重写compare方法，对issue中的option进行排序，用于计算predicted——value
            //System.out.println("getissue  "+issueweight.get(issue));
            //System.out.println("__________———————————_______________");
            for(ValueWeight valueWeight: issueweight.get(issue)){
                valueWeight.compute_predict(issueweight.get(issue).indexOf(valueWeight));//调用compute——predict方法，计算predict——value，因为列表的index从0开始，所以该函数需要对每一项都加一，用于计算compute函数计算
                valueWeight.compute();//计算weighthat值，论文中weighthat中该option的权重

//                System.out.println("V   "+valueWeight.V);
//                System.out.println("k   "+valueWeight.k);
//                System.out.println("predict_value   "+valueWeight.predicted_value);
//                System.out.println("weighthat   "+valueWeight.WeightHat);
//                System.out.println("+++++++++++++++++++++");

            }
            //System.out.println("________________________");
        }
        double totalWeight = 0.0f;//初始化该issue的weighthat
        for(Issue issue : myLastOffer.getIssues()){
            for (ValueWeight valueWeight : issueweight.get(issue)){
                totalWeight += valueWeight.WeightHat;//将option权重加和得出issue的weighthat
            }
        }
        for (Issue issue : myLastOffer.getIssues()){
            double issueWeight = 0;//初始化issue权重
            double maxWeightofIssue = 0;//初始化一个最大权重
            for (ValueWeight valueWeight:issueweight.get(issue)){
                if(valueWeight.WeightHat>maxWeightofIssue){
                    maxWeightofIssue = valueWeight.WeightHat;//寻找该issue的最大权重
                }
            }
            issueWeight = maxWeightofIssue/totalWeight;//按照论文公式，计算issue的最终权重
            weightList.put(issue,issueWeight);//存入iamap中
        }
        double utility = 0.0f;
        for (Issue issue : myLastOffer.getIssues()){
            int currentissue = issue.getNumber();
            for(ValueWeight valueWeight:issueweight.get(issue)){
                if (valueWeight.valueName.toString().equals(myLastOffer.getValue(currentissue).toString())){
                    utility += weightList.get(issue)*valueWeight.V;
                    break;//当匹配到valueweight时，得出utility，就可以跳出循环，因为每一个bid只用一个offer会被提出。
                }
            }
        }
        //System.out.println(BidNumberAlready + "效用"+utility);
    }

    public double calUtil(Bid bid) {
        double util = 0.0f;
        for (Issue issue : bid.getIssues()) {
            int num = issue.getNumber();
            for (ValueWeight valueWeight : issueweight.get(issue)) {
                if (valueWeight.valueName.toString().equals(bid.getValue(num).toString())) {
                    util += weightList.get(issue)*valueWeight.V;
                    break;
                }
            }
        }
        return util;
    }

    private Bid getMaxUtilityBid() {
        try {
            return this.utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private  Bid getMinUtility(){
        try{
            Bid bid = this.utilitySpace.getMinUtilityBid();
            return bid;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public  Bid getanother(double currentutil){
        Bid currentBid = null;
        double util = 0.0f;
        Bid maxbid = getMaxUtilityBid();
        double maxutil = utilitySpace.getUtility(maxbid);

        do {
            currentBid = generateRandomBid();
            util = utilitySpace.getUtility(currentBid);
        }while (util < maxutil && util >= currentutil);
        System.out.println("currentBid : "+currentBid);
        System.out.println("currentutil : "+currentutil);


        return currentBid;
    }
    public int getbid(){
        BidNumberAlready+= 1;
        return BidNumberAlready;
    }

//    public  Bid generateNewBid(){
//        Bid randomBid = null;
//        double upconcession = 0.0f;
//        double downconcession = 0.0f;
//        double time = getTimeLine().getTime();
//        double currentutil = 0.0f;
//        double yourutil = 0.0f;
//        double reserveutil = 0.2;
//        Bid maxBid = getMaxUtilityBid();
//        double Maxutil = utilitySpace.getUtility(maxBid);
//        int bidAlready = getbid();
//        if(time < 0.05){
//            return maxBid;
//        } else if (time<= 0.3 && time >= 0.05) {
//            upconcession = (double) (0.005 * bidAlready);
//            downconcession = upconcession + 0.05;
//            double predictuputil = Maxutil - upconcession;
//            double preductdowmutil = Maxutil - downconcession;
//            System.out.println(">>>concession is : "+upconcession);
//            do {
//                randomBid = generateRandomBid();
//                currentutil = utilitySpace.getUtility(randomBid);
//            }while (currentutil <= predictuputil && currentutil >= preductdowmutil);
//            return randomBid;
//        }else if (time <= 0.7){
//            yourutil = iaMap.calUtil(myLastOffer);
//            System.out.println(">>>yourutil is : "+yourutil);
//            do {
//                randomBid = generateRandomBid();
//                currentutil = utilitySpace.getUtility(randomBid);
//            }while (currentutil <= yourutil+0.05 && currentutil >= yourutil-0.1);
//            return randomBid;
//        } else if (time > 0.7 && time <=0.98) {
//            yourutil = iaMap.calUtil(myLastOffer);
//            System.out.println(">>>yourutil is : "+yourutil);
//            do {
//                randomBid = generateRandomBid();
//                currentutil = utilitySpace.getUtility(randomBid);
//            }while (currentutil > reserveutil && currentutil <= yourutil-0.1);
//            return randomBid;
//        }else {
//            do {
//                randomBid = generateRandomBid();
//                currentutil = utilitySpace.getUtility(randomBid);
//            }while (currentutil<= reserveutil+0.08 && currentutil >= reserveutil);
//            return randomBid;
//        }
//    }

//    public  Bid generateNewBid2(){
//        Bid currentbid = null;//当前Bid
//        Bid randombid = null;
//        HashMap<Integer,Bid> acceptablebid = new HashMap<>();//存储对手的bid（模拟数据）
//        List<Double> yourlist = new ArrayList<>();
//        double time = getTimeLine().getTime();
//        double concession = 0.0f;
//        int bidalready = getbid();
//        if (time<0.06){//当时间小于0.06时，我只输出最大的utility，欺骗对手，同时为john black争取数据（对手的bid）
//            currentbid = getMaxUtilityBid();
//            return currentbid;
//        }else if (time >= 0.05 && time < 0.5){//当时间在这个阈值时，设置自己的concession
//            concession = (double) (0.006*bidalready);//concession 按照每次0.006更新迭代
//            double targetutil = (double) (1 - concession);//计算concession之后的utility
//            List<Bid> bidlsit = new ArrayList<>();
//            List<Double> utilitylist = new ArrayList<>();
//            for(int i = 0; i<=2000;i++){
//                randombid = generateRandomBid();
//                double currentutil = utilitySpace.getUtility(randombid);//计算自己的utility
//                bidlsit.add(randombid);
//                utilitylist.add(currentutil);
//                if (currentutil>= (targetutil-0.1) && currentutil <= targetutil){//只要生成的bid的我的utility在我的可接受范围内，就加入到hashmap中
//                    acceptablebid.put(0,randombid);
//                    //System.out.println(">>>>>>>>>currentutil : "+currentutil);
//                    break;
//                }
//            }
//            if(acceptablebid.isEmpty()){//如果还是没找到，就直接给一个最大utility bid
//                double utility = Collections.max(utilitylist);
//                Integer index = utilitylist.indexOf(utility);
//                currentbid = bidlsit.get(index);
//                utilitylist.clear();
//                bidlsit.clear();
//                //currentbid = getMaxUtilityBid();
//                return  currentbid;
//            }else {//
//                currentbid = acceptablebid.get(0);
//                //System.out.println(">>>>currentbid : "+currentbid);
//                acceptablebid.clear();
//                return currentbid;
//            }
//        }else {//利用现有的资源和john black方法中的对手权重，模拟计算若干次Bid，存进Hashmap中
//            List<Bid> offerlist = new ArrayList<>();
//            for(int i = 0;i<1500;i++){
//                randombid = generateRandomBid();
//                double currentutil = utilitySpace.getUtility(randombid);
//                if(currentutil >= 0.6 && currentutil <= 0.8){
//                    double yourutil = calUtil(randombid);
//                    //double yourutil = iaMap.calUtil(randombid);
//                    //System.out.println("yourutil"+yourutil);
//                    yourlist.add(yourutil);
//                    offerlist.add(randombid);
//                }
//            }
//            if(offerlist.isEmpty()){
//                randombid = generateRandomBid();
//                return randombid;
//            }else {
//                double maxyourutil = Collections.max(yourlist);
//                int index = yourlist.indexOf(maxyourutil);
//                currentbid = offerlist.get(index);
//                yourlist.clear();
//                offerlist.clear();
//                return currentbid;
//            }
//        }
//    }

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
            concession = (double) (0.005*bidalready);//concession 按照每次0.006更新迭代
            double targetutil = (double) (1 - concession);//计算concession之后的utility
            List<Bid> bidlsit = new ArrayList<>();
            List<Double> utilitylist = new ArrayList<>();
            for(int i = 0; i<=2000;i++){
                randombid = generateRandomBid();
                double currentutil = utilitySpace.getUtility(randombid);//计算自己的utility
                bidlsit.add(randombid);
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
                currentbid = bidlsit.get(index);
                utilitylist.clear();
                bidlsit.clear();
                //currentbid = getMaxUtilityBid();
                return  currentbid;
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
                if(currentutil >= 0.67 && currentutil <= 0.85){
                    double yourutil = calUtil(randombid);
                    //double yourutil = iaMap.calUtil(randombid);
                    //System.out.println("yourutil"+yourutil);
                    double nashproduct = Calutilityproduct(currentutil,yourutil);
                    nashlist.add(nashproduct);
                    offerlist.add(randombid);
                }
            }
            if(offerlist.isEmpty()){
                randombid = generateRandomBid();
                return randombid;
            }else {
                double maxnashproduct = Collections.max(nashlist);
                //System.out.println("maxnashproduct"+maxnashproduct);
                int index = nashlist.indexOf(maxnashproduct);
                currentbid = offerlist.get(index);
                nashlist.clear();
                offerlist.clear();
                return currentbid;
            }
        }
    }


//    public  Bid generateNewBid4(){
//        Bid currentbid = null;//当前Bid
//        Bid randombid = null;
//        HashMap<Integer,Bid> acceptablebid = new HashMap<>();//存储对手的bid（模拟数据）
//        List<Double> alllist = new ArrayList<>();
//        double time = getTimeLine().getTime();
//        double concession = 0.0f;
//        int bidalready = getbid();
//        if (time<0.06){//当时间小于0.06时，我只输出最大的utility，欺骗对手，同时为john black争取数据（对手的bid）
//            currentbid = getMaxUtilityBid();
//            return currentbid;
//        }else if (time >= 0.05 && time < 0.5){//当时间在这个阈值时，设置自己的concession
//            concession = (double) (0.004*bidalready);//concession 按照每次0.006更新迭代
//            double targetutil = (double) (1 - concession);//计算concession之后的utility
//            List<Bid> bidlsit = new ArrayList<>();
//            List<Double> utilitylist = new ArrayList<>();
//            for(int i = 0; i<=2000;i++){
//                randombid = generateRandomBid();
//                double currentutil = utilitySpace.getUtility(randombid);//计算自己的utility
//                bidlsit.add(randombid);
//                utilitylist.add(currentutil);
//                if (currentutil>= (targetutil-0.1) && currentutil <= targetutil){//只要生成的bid的我的utility在我的可接受范围内，就加入到hashmap中
//                    acceptablebid.put(0,randombid);
//                    //System.out.println(">>>>>>>>>currentutil : "+currentutil);
//                    break;
//                }
//            }
//            if(acceptablebid.isEmpty()){//如果还是没找到，就直接给一个最大utility bid
//                double utility = Collections.max(utilitylist);
//                Integer index = utilitylist.indexOf(utility);
//                currentbid = bidlsit.get(index);
//                utilitylist.clear();
//                bidlsit.clear();
//                //currentbid = getMaxUtilityBid();
//                return  currentbid;
//            }else {//
//                currentbid = acceptablebid.get(0);
//                //System.out.println(">>>>currentbid : "+currentbid);
//                acceptablebid.clear();
//                return currentbid;
//            }
//        }else {//利用现有的资源和john black方法中的对手权重，模拟计算若干次Bid，存进Hashmap中
//            List<Bid> offerlist = new ArrayList<>();
//            for(int i = 0;i<2500;i++){
//                randombid = generateRandomBid();
//                double currentutil = utilitySpace.getUtility(randombid);
//                if(currentutil >= 0.7 && currentutil <= 0.9){
//                    double yourutil = calUtil(randombid);
//                    //double yourutil = iaMap.calUtil(randombid);
//                    //System.out.println("yourutil"+yourutil);
//                    double sw = socialWelfare(currentutil,yourutil);
//                    double na = Calutilityproduct(currentutil,yourutil);
//                    double allcal = Normalization(currentutil,sw,na);
//                    alllist.add(allcal);
//                    offerlist.add(randombid);
//                }
//            }
//            if(offerlist.isEmpty()){
//                randombid = generateRandomBid();
//                return randombid;
//            }else {
//                double maxnashproduct = Collections.max(alllist);
//                //System.out.println("maxnashproduct"+maxnashproduct);
//                int index = alllist.indexOf(maxnashproduct);
//                currentbid = offerlist.get(index);
//                alllist.clear();
//                offerlist.clear();
//                return currentbid;
//            }
//        }
//    }

    public double Calutilityproduct(double myutility , double opponentutility){
        return myutility*opponentutility;
    }

//    public double socialWelfare(double myutiliy, double opponentutility){
//        return myutiliy+opponentutility;
//    }
//
//    public double Normalization(double myutility, double sw, double na){
//        return (myutility - na)/(sw-na);
//    }






}
