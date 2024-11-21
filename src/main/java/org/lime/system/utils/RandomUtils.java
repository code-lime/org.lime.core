package org.lime.system.utils;

import org.lime.system.tuple.Tuple2;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomUtils {
    private final static Random rnd = new Random();

    public static int rand(int min, int max) {
        int _min = Math.min(min, max);
        int _max = Math.max(min, max);
        return rnd.nextInt((_max - _min) + 1) + _min;
    }
    public static int rand(Tuple2<Integer, Integer> minmax) { return rand(minmax.val0, minmax.val1); }
    public static double rand(double min, double max) {
        double _min = Math.min(min, max);
        double _max = Math.max(min, max);
        return _min + (_max - _min) * rnd.nextDouble();
    }
    public static <T> T rand(Collection<T> array) {
        Object[] arr = array.toArray();
        return (T) arr[rand(0, arr.length - 1)];
    }
    public static boolean rand_is(double value) {
        return !(value <= 0) && (value >= 1 || rnd.nextDouble() <= value);
    }
    public static boolean rand() { return rand_is(0.5); }
    public static <T>T rand(T... array) { return array[rand(0, array.length - 1)]; }
    public static <T>void randomize(List<T> list) { Collections.shuffle(list, rnd); }
}
