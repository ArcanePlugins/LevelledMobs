package io.github.lokka30.levelledmobs.utils;

import java.util.Arrays;
import java.util.List;

public class Utils {

    public Utils() {
    }

    public List<String> getSupportedServerVersions() {
        return Arrays.asList("1.15", "1.16");
    }

    public int getLatestSettingsVersion() {
        return 20;
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
