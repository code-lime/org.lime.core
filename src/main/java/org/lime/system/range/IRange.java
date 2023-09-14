package org.lime.system.range;

import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class IRange {
    public static IRange parse(String text) {
        if (text.contains(","))
            return new ListRange(Arrays.stream(text.split(",")).map(IRange::parse).toList());
        String[] damageList = text.replace("..", ":").split(":");

        if (damageList.length == 1) {
            String prefix = damageList[0];
            int length = prefix.length();
            return prefix.endsWith("%")
                    ? new PercentRange(Double.parseDouble(prefix.substring(0, length - 1)) / 100)
                    : new OnceRange(Double.parseDouble(prefix));
        }

        return new DoubleRange(parse(damageList[0]), parse(damageList[1]));
    }

    public abstract double getMin(double max);

    public abstract double getMax(double max);

    public abstract double getValue(double max);

    public int getIntValue(double max) {
        return (int) Math.round(getValue(max));
    }

    public abstract String displayText();

    public abstract IntStream getAllInts(double max);

    public abstract boolean hasInt(double max, int value);

    public boolean inRange(double value, double max) {
        double _min = getMin(max);
        double _max = getMax(max);

        return value <= _max && value >= _min;
    }
}
