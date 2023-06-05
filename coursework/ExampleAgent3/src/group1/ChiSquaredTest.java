package group1;

import agents.anac.y2019.harddealer.math3.distribution.ChiSquaredDistribution;

import java.util.HashMap;
import java.util.Map;

/***
 * ChiSquaredTest
 */
public class ChiSquaredTest {
    private double[][] data;
    private int tmp_data;
    ChiSquaredDistribution cs;
    public ChiSquaredTest(double[][] data) {
        this.data = data;
        tmp_data =(data.length-1)*(data[0].length-1);
        cs=new ChiSquaredDistribution(tmp_data);
    }
    public double calculateChisquare() {
        double sum_tmp=0;
        Map<Integer,Double> col_data=new HashMap<>();
        Map<Integer,Double> row_data=new HashMap<>();
        for(int i=0;i<data.length;i++) {
            for(int j=0;j<data[i].length;j++) {
                sum_tmp=sum_tmp+data[i][j];
                checkData(col_data, j, data[i][j]);
                checkData(row_data, i, data[i][j]);
            }
        }
        double chisquare=0;
        for(int i=0;i<data.length;i++) {
            for(int j=0;j<data[i].length;j++) {
                double col_sum= col_data.get(j);
                double percent=col_sum/sum_tmp;
                double row_sum=row_data.get(i);
                double expect=row_sum*percent;
                chisquare=chisquare+(Math.pow(data[i][j]-expect, 2)/expect);
            }
        }
        return chisquare;
    }
    private void checkData(Map<Integer, Double> data, int key, double v) {
        Double d1 = data.get(key);
        if(d1!=null) {
            data.put(key, d1+v);
        }else {
            data.put(key, v);
        }
    }
    public double getPValue() {
        double chi=calculateChisquare();
        return cs.cumulativeProbability(chi);
    }
}