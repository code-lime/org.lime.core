package org.lime.core.common.utils.proxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseProxyImpl<T, Impl>
        implements BaseProxy<T, Impl> {
    private final Map<T, T> proxyItems = new ConcurrentHashMap<>();

    private final Impl proxy;

    public BaseProxyImpl(Impl proxy) {
        this.proxy = proxy;
    }

    @Override
    public Map<T, T> proxyItems() {
        return proxyItems;
    }
    @Override
    public Impl proxy() {
        return proxy;
    }
}
