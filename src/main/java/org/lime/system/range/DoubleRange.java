package org.lime.system.range;

import org.lime.system.utils.RandomUtils;

import java.util.stream.IntStream;

public class DoubleRange extends IRange {
    public final IRange from;
    public final IRange to;

    public DoubleRange(IRange from, IRange to) {
        this.from = from;
        this.to = to;
    }

    @Override public double getMin(double max) {
        return Math.min(from.getMin(max), to.getMin(max));
    }
    @Override public double getMax(double max) {
        return Math.max(from.getMax(max), to.getMax(max));
    }
    @Override public double getValue(double max) {
        double _v1 = from.getValue(max);
        double _v2 = to.getValue(max);
        return RandomUtils.rand(Math.min(_v1, _v2), Math.max(_v1, _v2));
    }
    @Override public String displayText() { return from.displayText() + " - " + to.displayText(); }
    @Override public IntStream getAllInts(double max) {
        double _v1 = from.getValue(max);
        double _v2 = to.getValue(max);
        int from = (int) Math.round(Math.min(_v1, _v2));
        int to = (int) Math.round(Math.max(_v1, _v2));
        return IntStream.rangeClosed(from, to);
    }
    @Override public boolean hasInt(double max, int value) {
        double _v1 = from.getValue(max);
        double _v2 = to.getValue(max);
        int from = (int) Math.round(Math.min(_v1, _v2));
        int to = (int) Math.round(Math.max(_v1, _v2));
        return from <= value && value <= to;
    }
    @Override public boolean inRange(double value, double max) {
        double _min = getMin(max);
        double _max = getMax(max);

        return value <= _max && value >= _min;
    }
    @Override public String toFormat() { return from.toFormat() + ".." + to.toFormat(); }
}
