package org.lime.core.common.utils.proxy;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public interface BaseProxyIterable<T, Impl extends Iterable<T>>
        extends Iterable<T>, BaseProxy<T, Impl> {
    @Override
    default @NotNull Iterator<T> iterator() {
        return Iterators.transform(proxy().iterator(), this::mapItem);
    }

    abstract class Impl<T, Impl extends Iterable<T>>
            extends BaseProxyImpl<T, Impl>
            implements BaseProxyIterable<T, Impl> {
        public Impl(Impl proxy) {
            super(proxy);
        }
    }
}
