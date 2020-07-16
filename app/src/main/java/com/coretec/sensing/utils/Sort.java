package com.coretec.sensing.utils;

import java.util.Comparator;

public class Sort {
    public static class Descending implements Comparator<Double> {
        @Override
        public int compare(Double o1, Double o2) {
            return o2.compareTo(o1);
        }
    }

    // 오름차순
    public static class Ascending implements Comparator<Double> {
        @Override
        public int compare(Double o1, Double o2) {
            return o1.compareTo(o2);
        }

    }
}
