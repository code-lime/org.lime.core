package org.lime.system.range;

import java.util.stream.IntStream;

public class PercentRange extends IRange {
    public final double value;

    public PercentRange(double value) { this.value = value; }

    @Override public double getMin(double max) { return value * max; }
    @Override public double getMax(double max) { return value * max; }
    @Override public double getValue(double max) { return value * max; }
    @Override public String displayText() { return (int) (value * 100) + "%"; }
    @Override public IntStream getAllInts(double max) { return IntStream.of(getIntValue(max)); }
    @Override public boolean hasInt(double max, int value) { return getIntValue(max) == value; }
    @Override public boolean inRange(double value, double max) { return value == getValue(max); }
    @Override public String toFormat() { return value * 100 + "%"; }
}
