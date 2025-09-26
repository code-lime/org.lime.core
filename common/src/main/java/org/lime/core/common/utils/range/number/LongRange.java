package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;

public final class LongRange
        extends BaseNumberRange<LongRange, Long> {
    public static final Factory<LongRange, Long> FACTORY = createFactory(LongRange.class, LongRange::new);

    public LongRange(@NotNull Long a, @NotNull Long b) {
        super(a, b, FACTORY);
    }

    @Override
    public @NotNull Long percent(double percent) {
        return Math.round(min + (max - min) * percent);
    }

    public static LongRange of(long a, long b) {
        return new LongRange(a, b);
    }
}
