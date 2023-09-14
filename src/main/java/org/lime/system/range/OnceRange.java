package org.lime.system.range;

import java.util.stream.IntStream;

public class OnceRange extends IRange {
    public final double value;

    public OnceRange(double value) {
        this.value = value;
    }

    @Override
    public double getMin(double max) {
        return value;
    }

    @Override
    public double getMax(double max) {
        return value;
    }

    @Override
    public double getValue(double max) {
        return value;
    }

    @Override
    public String displayText() {
        return String.valueOf(value);
    }

    @Override
    public IntStream getAllInts(double max) {
        return IntStream.of(getIntValue(max));
    }

    @Override
    public boolean hasInt(double max, int value) {
        return getIntValue(max) == value;
    }
}
