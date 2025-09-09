package org.lime.core.common.utils.range.number;

import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.range.BaseRange;

public class BaseNumberRange<Self extends BaseNumberRange<Self, T>, T extends Number & Comparable<T>>
        extends BaseRange<Self, T> {
    protected BaseNumberRange(@NotNull T a, @NotNull T b, Factory<Self, T> factory) {
        super(a, b, factory);
    }
}
