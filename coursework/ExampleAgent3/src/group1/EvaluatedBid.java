package group1;

import genius.core.Bid;
public class EvaluatedBid {
    public Bid bid;
    public double myutility;
    public double opponentutility;

    public EvaluatedBid(Bid bid, double myutility, double opponentutility){
        this.bid = bid;
        this.myutility = myutility;
        this.opponentutility = opponentutility;
    }

    public Bid getBid() {
        return bid;
    }

    public double getMyutility() {
        return myutility;
    }

    public double getOpponentutility() {
        return opponentutility;
    }

    public void setBid(Bid bid) {
        this.bid = bid;
    }

    public void setMyutility(double myutility) {
        this.myutility = myutility;
    }

    public void setOpponentutility(double opponentutility) {
        this.opponentutility = opponentutility;
    }

}
