package org.lime.core.common.utils.range;

public interface FactoryRange<Self extends FactoryRange<Self, T>, T extends Comparable<T>>
        extends Range<T> {
    Factory<Self, T> factory();
}
