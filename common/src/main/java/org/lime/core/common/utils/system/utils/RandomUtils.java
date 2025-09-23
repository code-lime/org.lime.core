package org.lime.core.common.utils.system.utils;

import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.range.number.DoubleRange;
import org.lime.core.common.utils.range.number.FloatRange;
import org.lime.core.common.utils.range.number.IntegerRange;
import org.lime.core.common.utils.range.number.LongRange;
import org.lime.core.common.utils.system.execute.Func1;

import java.util.*;

public class RandomUtils {
    private final static Random rnd = new Random();

    public static <T>Optional<T> weighted(
            final @NotNull Collection<@NotNull T> items,
            final @NotNull Func1<@NotNull T, Double> weightCalculator) {
        double sum = 0.0;
        for (T item : items)
            sum += weightCalculator.invoke(item);

        if (sum <= 0.0)
            return Optional.empty();

        double randomizedSum = rnd.nextDouble() * sum;
        double cumulative = 0.0;
        T last = null;

        for (T item : items) {
            double w = weightCalculator.invoke(item);
            cumulative += w;
            if (randomizedSum < cumulative)
                return Optional.of(item);
            last = item;
        }
        return Optional.ofNullable(last);
    }
    public static <T>Optional<T> weighted(
            final @NotNull Map<T, Double> weights) {
        return weighted(weights.entrySet(), Map.Entry::getValue)
                .map(Map.Entry::getKey);
    }

    public static boolean chance(double chance) {
        return chance > 0 && (chance >= 1 || rnd.nextDouble() <= chance);
    }
    public static boolean bool() {
        return rnd.nextBoolean();
    }

    public static <T>Optional<T> at(Collection<T> items) {
        if (items instanceof List<T> list)
            return at(list);
        int length = items.size();
        if (length > 0) {
            int at = rnd.nextInt(length);

            int i = 0;
            for (T item : items) {
                if (at == i)
                    return Optional.of(item);
                i++;
            }
        }
        return Optional.empty();
    }
    public static <T>Optional<T> at(List<T> items) {
        int length = items.size();
        return length > 1
                ? Optional.of(items.get(rnd.nextInt(length)))
                : Optional.empty();
    }
    @SafeVarargs
    public static <T>Optional<T> at(T... items) {
        int length = items.length;
        return length > 1
                ? Optional.of(items[rnd.nextInt(length)])
                : Optional.empty();
    }

    public static int inclusive(int min, int max) {
        return rnd.nextInt(max - min + 1) + min;
    }
    public static int exclusive(int min, int max) {
        if (max - min <= 1)
            throw new IllegalArgumentException("Small delta of (min; max)");
        return rnd.nextInt(max - min - 1) + min + 1;
    }
    public static int inclusive(IntegerRange range) {
        return inclusive(range.min(), range.max());
    }
    public static int exclusive(IntegerRange range) {
        return exclusive(range.min(), range.max());
    }

    public static long inclusive(long min, long max) {
        return rnd.nextLong(max - min + 1) + min;
    }
    public static long exclusive(long min, long max) {
        if (max - min <= 1)
            throw new IllegalArgumentException("Small delta of (min; max)");
        return rnd.nextLong(max - min - 1) + min + 1;
    }
    public static long inclusive(LongRange range) {
        return inclusive(range.min(), range.max());
    }
    public static long exclusive(LongRange range) {
        return exclusive(range.min(), range.max());
    }

    public static float inclusive(float min, float max) {
        return min + (max - min) * rnd.nextFloat();
    }
    public static float exclusive(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("min >= max");
        }
        float r;
        do {
            r = min + (max - min) * rnd.nextFloat();
        } while (r == min || r == max);
        return r;
    }
    public static float inclusive(FloatRange range) {
        return inclusive(range.min(), range.max());
    }
    public static float exclusive(FloatRange range) {
        return exclusive(range.min(), range.max());
    }

    public static double inclusive(double min, double max) {
        return min + (max - min) * rnd.nextDouble();
    }
    public static double exclusive(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("min >= max");
        }
        double r;
        do {
            r = min + (max - min) * rnd.nextDouble();
        } while (r == min || r == max);
        return r;
    }
    public static double inclusive(DoubleRange range) {
        return inclusive(range.min(), range.max());
    }
    public static double exclusive(DoubleRange range) {
        return exclusive(range.min(), range.max());
    }
}
