package org.lime.core.common.utils.range;

import org.jetbrains.annotations.NotNull;

public final class FloatRange
        extends BaseRange<FloatRange, Float> {
    public static final Factory<FloatRange, Float> FACTORY = createFactory(FloatRange.class, FloatRange::new);

    public FloatRange(@NotNull Float a, @NotNull Float b) {
        super(a, b, FACTORY);
    }

    public static FloatRange of(float a, float b) {
        return new FloatRange(a, b);
    }
}
