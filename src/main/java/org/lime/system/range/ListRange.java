package org.lime.system.range;

import org.lime.system.utils.RandomUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListRange extends IRange {
    public final List<IRange> list;

    public ListRange(List<IRange> list) {
        this.list = list;
    }

    @Override public double getMin(double max) {
        return list.stream().mapToDouble(v -> v.getMin(max)).min().orElse(0);
    }

    @Override public double getMax(double max) {
        return list.stream().mapToDouble(v -> v.getMax(max)).max().orElse(0);
    }

    @Override public double getValue(double max) {
        return RandomUtils.rand(list).getValue(max);
    }

    @Override public String displayText() {
        return list.stream().map(IRange::displayText).collect(Collectors.joining(", "));
    }

    @Override public IntStream getAllInts(double max) {
        return list.stream()
                .flatMapToInt(v -> v.getAllInts(max))
                .distinct()
                .sorted();
    }
    @Override public boolean hasInt(double max, int value) {
        for (IRange part : list) {
            if (part.hasInt(max, value))
                return true;
        }
        return false;
    }
    @Override public boolean inRange(double value, double max) {
        for (IRange part : list) {
            if (part.inRange(value, max))
                return true;
        }
        return false;
    }
    @Override public String toFormat() {
        return list.stream().map(IRange::toFormat).collect(Collectors.joining(","));
    }
}
