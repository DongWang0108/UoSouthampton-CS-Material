package group1;

import genius.core.issue.Value;

import java.util.*;

public class ValueNew implements Comparator<ValueNew> {
    public Value valueName;
    public int count=0;

    public ValueNew(Value valueName) {
        this.valueName = valueName;
    }

    @Override
    public String toString() {
        return "[" +this.valueName+"]="+Integer.toString(this.count) ;
    }

    //我们需要根据count数目进行排序
    @Override
    public int compare(ValueNew o1, ValueNew o2) {
        if(o1.count < o2.count){
            return 1;
        }else{
            return -1;
        }
    }

}