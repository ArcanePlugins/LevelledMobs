package io.github.lokka30.levelledmobs.utils;

public class Utils {

    public Utils() {
    }

    //Factorial function
    public static long factorial(int num) {
        long result = 1;
        for (int i = num; i > 1; i--)
            result *= i;
        return result;
    }

    public String getRecommendedServerVersion() {
        return "1.15";
    }

    public int getRecommendedSettingsVersion() {
        return 16;
    }

    //This is a method created by Jonik & Mustapha Hadid at StackOverflow.
    //It simply grabs 'value', being a double, and rounds it, leaving 'places' decimal places intact.
    //Created by Jonik & Mustapha Hadid @ stackoverflow
    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    //Creates a weighted array where the values contain the sum of itself and all preceding values
    public double[] createWeightedArray(double[] inputarray) {
        double[] outputarray = new double[inputarray.length];

        outputarray[0] = inputarray[0];
        for (int i = 1; i < inputarray.length; i++) {
            outputarray[i] = inputarray[i] + outputarray[i - 1];
        }

        return outputarray;
    }

    //Binomial distribution function
    public double binomialDistribution(int n, int k, double p) {
        return ((double) factorial(n)) / ((double) (factorial(k)) * ((double) factorial(n - k))) * Math.pow(p, k) * Math.pow(1 - p, n - k);
    }

    //Integer check
    public boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    //Integer check
    public boolean isInteger(String s, int radix) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }


}
