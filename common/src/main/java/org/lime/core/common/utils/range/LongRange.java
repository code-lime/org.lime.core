package org.lime.core.common.utils.range;

import org.jetbrains.annotations.NotNull;

public final class LongRange
        extends BaseRange<LongRange, Long> {
    public static final Factory<LongRange, Long> FACTORY = createFactory(LongRange.class, LongRange::new);

    public LongRange(@NotNull Long a, @NotNull Long b) {
        super(a, b, FACTORY);
    }

    public static LongRange of(long a, long b) {
        return new LongRange(a, b);
    }
}
