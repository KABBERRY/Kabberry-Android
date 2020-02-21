package com.kabberry.wallet.util;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class PasswordUtil {
    private static PasswordUtil instance = null;
    private static HashMap<Pattern, Double> patternsQuality = null;
    private static HashMap<Pattern, Double> patternsWeight = null;

    private PasswordUtil() {
    }

    public static PasswordUtil getInstance() {
        if (instance == null) {
            patternsWeight = new HashMap();
            patternsWeight.put(Pattern.compile("^\\d+$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[a-z]+\\d$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[A-Z]+\\d$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[a-zA-Z]+\\d$"), Double.valueOf(0.5d));
            patternsWeight.put(Pattern.compile("^[a-z]+\\d+$"), Double.valueOf(0.5d));
            patternsWeight.put(Pattern.compile("^[a-z]+$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[A-Z]+$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[A-Z][a-z]+$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[A-Z][a-z]+\\d$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[A-Z][a-z]+\\d+$"), Double.valueOf(0.5d));
            patternsWeight.put(Pattern.compile("^[a-z]+[._!\\- @*#]$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[A-Z]+[._!\\- @*#]$"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("^[a-zA-Z]+[._!\\- @*#]$"), Double.valueOf(0.5d));
            patternsWeight.put(Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"), Double.valueOf(0.25d));
            patternsWeight.put(Pattern.compile("_^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$_iuS"), Double.valueOf(0.5d));
            patternsQuality = new HashMap();
            patternsQuality.put(Pattern.compile(".*\\d.*"), Double.valueOf(10.0d));
            patternsQuality.put(Pattern.compile(".*[a-z].*"), Double.valueOf(26.0d));
            patternsQuality.put(Pattern.compile(".*[A-Z].*"), Double.valueOf(26.0d));
            patternsQuality.put(Pattern.compile(".*[^a-zA-Z0-9 ].*"), Double.valueOf(31.0d));
            instance = new PasswordUtil();
        }
        return instance;
    }

    public double getStrength(String pw) {
        return Math.min(getQuality(pw) * log2(Math.pow(getBase(pw), (double) pw.length())), 100.0d);
    }

    private static double getBase(String pw) {
        double base = 1.0d;
        for (Entry item : patternsQuality.entrySet()) {
            if (((Pattern) item.getKey()).matcher(pw).matches()) {
                base += ((Double) item.getValue()).doubleValue();
            }
        }
        return base;
    }

    private static double log2(double a) {
        return Math.log(a) / Math.log(2.0d);
    }

    private static double getQuality(String pw) {
        double weight = 1.0d;
        for (Entry item : patternsWeight.entrySet()) {
            if (((Pattern) item.getKey()).matcher(pw).matches()) {
                weight = Math.min(weight, ((Double) item.getValue()).doubleValue());
            }
        }
        return weight;
    }
}
