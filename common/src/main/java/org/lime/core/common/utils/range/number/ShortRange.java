package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;

public final class ShortRange
        extends BaseNumberRange<ShortRange, Short> {
    public static final Factory<ShortRange, Short> FACTORY = createFactory(ShortRange.class, ShortRange::new);

    public ShortRange(@NotNull Short a, @NotNull Short b) {
        super(a, b, FACTORY);
    }

    public static ShortRange of(short a, short b) {
        return new ShortRange(a, b);
    }
}
