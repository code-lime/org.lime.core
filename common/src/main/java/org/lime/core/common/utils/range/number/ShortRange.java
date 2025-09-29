package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;

public final class ShortRange
        extends BaseNumberRange<ShortRange, Short> {
    public static final Factory<ShortRange, Short> FACTORY = createFactory(ShortRange.class, ShortRange::new);

    public ShortRange(@NotNull Short a, @NotNull Short b) {
        super(a, b, FACTORY);
    }

    @Override
    public @NotNull Short percent(double percent) {
        return (short)Math.round(min + (max - min) * percent);
    }

    public static ShortRange of(short a, short b) {
        return new ShortRange(a, b);
    }
    public static ShortRange of(short value) {
        return of(value, value);
    }
}
