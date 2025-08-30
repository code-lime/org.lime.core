package org.lime.core.common.utils.system.range;

import java.util.stream.IntStream;

public class OnceRange extends BaseRange {
    public final double value;

    public OnceRange(double value) { this.value = value; }

    @Override public double getMin(double max) { return value; }
    @Override public double getMax(double max) { return value; }
    @Override public double getValue(double max) { return value; }
    @Override public String displayText() { return String.valueOf(value); }
    @Override public IntStream getAllInts(double max) { return IntStream.of(getIntValue(max)); }
    @Override public boolean hasInt(double max, int value) { return getIntValue(max) == value; }
    @Override public boolean inRange(double value, double max) { return value == getValue(max); }
    @Override public String toFormat() { return String.valueOf(value); }
}
