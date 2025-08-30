package org.lime.core.common.utils.system;

import org.lime.core.common.utils.system.execute.Func1;
import org.lime.core.common.utils.system.tuple.Tuple2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapBuilder<TKey, TValue> {
    private final HashMap<TKey, TValue> map;

    private MapBuilder() {
        this(false);
    }

    private MapBuilder(boolean linked) {
        this(linked ? new LinkedHashMap<>() : new HashMap<>());
    }

    private MapBuilder(HashMap<TKey, TValue> map) {
        this.map = map;
    }

    public MapBuilder<TKey, TValue> add(TKey key, TValue value) {
        this.map.put(key, value);
        return this;
    }

    public MapBuilder<TKey, TValue> add(Map.Entry<TKey, TValue> entry) {
        return add(entry.getKey(), entry.getValue());
    }

    public MapBuilder<TKey, TValue> add(Tuple2<TKey, TValue> entry) {
        return add(entry.val0, entry.val1);
    }

    public MapBuilder<TKey, TValue> add(Map<TKey, TValue> map) {
        this.map.putAll(map);
        return this;
    }

    public MapBuilder<TKey, TValue> add(Iterable<Tuple2<TKey, TValue>> map) {
        map.forEach(this::add);
        return this;
    }

    public MapBuilder<TKey, TValue> add(Iterable<TKey> keys, TValue value) {
        keys.forEach(key -> add(key, value));
        return this;
    }

    public MapBuilder<TKey, TValue> add(Iterator<Tuple2<TKey, TValue>> map) {
        map.forEachRemaining(this::add);
        return this;
    }

    public MapBuilder<TKey, TValue> add(Iterator<TKey> keys, TValue value) {
        keys.forEachRemaining(key -> add(key, value));
        return this;
    }

    public <T> MapBuilder<TKey, TValue> add(Iterable<T> list, Func1<T, TKey> key, Func1<T, TValue> value) {
        list.forEach(item -> add(key.invoke(item), value.invoke(item)));
        return this;
    }

    public <T> MapBuilder<TKey, TValue> add(Iterator<T> list, Func1<T, TKey> key, Func1<T, TValue> value) {
        list.forEachRemaining(item -> add(key.invoke(item), value.invoke(item)));
        return this;
    }

    public <TTKey, TTValue> MapBuilder<TKey, TValue> add(Map<TTKey, TTValue> map, Func1<TTKey, TKey> key, Func1<TTValue, TValue> value) {
        map.forEach((k, v) -> add(key.invoke(k), value.invoke(v)));
        return this;
    }

    public HashMap<TKey, TValue> build() {
        return map;
    }

    public MapBuilder<TKey, TValue> copy() {
        return new MapBuilder<TKey, TValue>().add(map);
    }


    public static <TKey, TValue> MapBuilder<TKey, TValue> of(Class<TKey> tKey, Class<TValue> tValue) {
        return new MapBuilder<>();
    }

    public static <TKey, TValue> MapBuilder<TKey, TValue> of() {
        return new MapBuilder<>();
    }

    public static <TKey, TValue> MapBuilder<TKey, TValue> of(TKey key, TValue value) {
        return MapBuilder.<TKey, TValue>of().add(key, value);
    }

    public static <TKey, TValue> MapBuilder<TKey, TValue> of(Class<TKey> tKey, Class<TValue> tValue, boolean linked) {
        return new MapBuilder<>(linked);
    }

    public static <TKey, TValue> MapBuilder<TKey, TValue> of(boolean linked) {
        return new MapBuilder<>(linked);
    }

    public static <TKey, TValue> MapBuilder<TKey, TValue> of(TKey key, TValue value, boolean linked) {
        return MapBuilder.<TKey, TValue>of(linked).add(key, value);
    }

    public static <TKey, TValue> MapBuilder<TKey, TValue> of(HashMap<TKey, TValue> value) {
        return new MapBuilder<>(value);
    }
}
