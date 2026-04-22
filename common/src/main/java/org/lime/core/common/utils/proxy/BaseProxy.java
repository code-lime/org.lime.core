package org.lime.core.common.utils.proxy;

import java.util.Map;

public interface BaseProxy<T, Impl> {
    Map<T, T> proxyItems();

    Impl proxy();

    T creatProxyItem(T current);

    default T mapItem(T current) {
        return proxyItems().computeIfAbsent(current, this::creatProxyItem);
    }
    default Object mapObjectItem(Object current) {
        //noinspection unchecked
        return mapItem((T)current);
    }
}
