package org.lime.core.common.api.elements;

public enum SortType {
    First(Double.NEGATIVE_INFINITY),
    Default(0),
    Last(Double.POSITIVE_INFINITY);

    private final double value;

    SortType(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
