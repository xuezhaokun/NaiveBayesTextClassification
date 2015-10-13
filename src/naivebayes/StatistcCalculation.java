package naivebayes;

import java.util.List;
/**
 * helper class for calculating average and standard deviation
 * @author Zhaokun Xue
 *
 */
public class StatistcCalculation {

    public static double getMean(List<Double> data){
        double sum = 0.0;
        int size = data.size();
        for(double a : data){
            sum += a;
        }
        return sum/size;
    }

    public static double getVariance(List<Double> data){
        double mean = getMean(data);
        double size = data.size();
        double temp = 0;
        for(double a :data){
            temp += (mean-a)*(mean-a);
        }
        return temp/size;
    }

    public static double getStdDev(List<Double> data){
        return Math.sqrt(getVariance(data));
    }
}
