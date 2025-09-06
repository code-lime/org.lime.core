package org.lime.core.common.utils.range;

import org.jetbrains.annotations.NotNull;

public final class DoubleRange
        extends BaseRange<DoubleRange, Double> {
    public static final Factory<DoubleRange, Double> FACTORY = createFactory(DoubleRange.class, DoubleRange::new);

    public DoubleRange(@NotNull Double a, @NotNull Double b) {
        super(a, b, FACTORY);
    }

    public static DoubleRange of(double a, double b) {
        return new DoubleRange(a, b);
    }
}
