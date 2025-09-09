package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;

public final class ByteRange
        extends BaseNumberRange<ByteRange, Byte> {
    public static final Factory<ByteRange, Byte> FACTORY = createFactory(ByteRange.class, ByteRange::new);

    public ByteRange(@NotNull Byte a, @NotNull Byte b) {
        super(a, b, FACTORY);
    }

    public static ByteRange of(byte a, byte b) {
        return new ByteRange(a, b);
    }
}
