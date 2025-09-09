package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;

public final class IntegerRange
        extends BaseNumberRange<IntegerRange, Integer> {
    public static final Factory<IntegerRange, Integer> FACTORY = createFactory(IntegerRange.class, IntegerRange::new);

    public IntegerRange(@NotNull Integer a, @NotNull Integer b) {
        super(a, b, FACTORY);
    }

    public static IntegerRange of(int a, int b) {
        return new IntegerRange(a, b);
    }
}
