package org.lime.core.common.api;

import org.apache.commons.lang3.StringUtils;
import org.lime.core.common.system.execute.Action2;
import org.lime.core.common.system.execute.Action3;

import java.util.Collection;
import java.util.Map;

public interface BaseInvokeProgress {
    default <T>void invokeList(Collection<T> list, Action2<T, String> invoke) {
        int size = list.size();
        int i = 0;
        for (T item : list) {
            invoke.invoke(item, "[" + StringUtils.leftPad(String.valueOf(i*100 / size), 3, '*').replace("*", "...") + "%]");
            i++;
        }
    }
    default <K, V>void invokeList(Map<K, V> list, Action3<K, V, String> invoke) {
        invokeList(list.entrySet(), (v,pref) -> invoke.invoke(v.getKey(), v.getValue(), pref));
    }
}
