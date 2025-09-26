package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;

public final class DoubleRange
        extends BaseNumberRange<DoubleRange, Double> {
    public static final Factory<DoubleRange, Double> FACTORY = createFactory(DoubleRange.class, DoubleRange::new);

    public DoubleRange(@NotNull Double a, @NotNull Double b) {
        super(a, b, FACTORY);
    }

    @Override
    public @NotNull Double percent(double percent) {
        return min + (max - min) * percent;
    }

    public static DoubleRange of(double a, double b) {
        return new DoubleRange(a, b);
    }
}
