import genius.core.issue.Value;
import java.util.Comparator;

public class ValueWeight implements Comparator<ValueWeight> {
    public  Value valueName;
    public int count = 0;//该option出现了多少次
    public int predicted_value = 0;//公式中的n
    public int k = 0;// issue中的选项
    public int BidAlready = 0;//已经有多少bid被提出
    public double V = 0.0f;//The calculated value
    public double WeightHat = 0.0f;//该option的权重

    public ValueWeight(Value valueName){
        this.valueName = valueName;
    }

    public void compute_predict(int rank){
        int current_index = rank + 1;
        this.predicted_value = current_index;
    }

    public void compute(){
       this.V = ((double)(this.k - this.predicted_value+1)/this.k);//曾经忘了加double
       this.WeightHat = Math.pow(((double)this.count/(double)this.BidAlready),2);

    }

    @Override
    public int compare(ValueWeight o1, ValueWeight o2) {
        if (o1.count < o2.count){
            return 1;
        }else{
            return -1;
        }
    }
}
