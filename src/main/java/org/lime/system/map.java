package org.lime.system;

import org.lime._system;
import org.lime.system.execute.Func1;
import org.lime.system.toast.Toast2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class map {
    public static class builder<TKey, TValue> {
        private final HashMap<TKey, TValue> map;

        private builder() {
            this(false);
        }

        private builder(boolean linked) {
            this(linked ? new LinkedHashMap<>() : new HashMap<>());
        }

        private builder(HashMap<TKey, TValue> map) {
            this.map = map;
        }

        public builder<TKey, TValue> add(TKey key, TValue value) {
            this.map.put(key, value);
            return this;
        }

        public builder<TKey, TValue> add(Map.Entry<TKey, TValue> entry) {
            return add(entry.getKey(), entry.getValue());
        }

        public builder<TKey, TValue> add(Toast2<TKey, TValue> entry) {
            return add(entry.val0, entry.val1);
        }

        public builder<TKey, TValue> add(Map<TKey, TValue> map) {
            this.map.putAll(map);
            return this;
        }

        public builder<TKey, TValue> add(Iterable<Toast2<TKey, TValue>> map) {
            map.forEach(this::add);
            return this;
        }

        public builder<TKey, TValue> add(Iterable<TKey> keys, TValue value) {
            keys.forEach(key -> add(key, value));
            return this;
        }

        public builder<TKey, TValue> add(Iterator<Toast2<TKey, TValue>> map) {
            map.forEachRemaining(this::add);
            return this;
        }

        public builder<TKey, TValue> add(Iterator<TKey> keys, TValue value) {
            keys.forEachRemaining(key -> add(key, value));
            return this;
        }

        public <T> builder<TKey, TValue> add(Iterable<T> list, Func1<T, TKey> key, Func1<T, TValue> value) {
            list.forEach(item -> add(key.invoke(item), value.invoke(item)));
            return this;
        }

        public <T> builder<TKey, TValue> add(Iterator<T> list, Func1<T, TKey> key, Func1<T, TValue> value) {
            list.forEachRemaining(item -> add(key.invoke(item), value.invoke(item)));
            return this;
        }

        public <TTKey, TTValue> builder<TKey, TValue> add(Map<TTKey, TTValue> map, Func1<TTKey, TKey> key, Func1<TTValue, TValue> value) {
            map.forEach((k, v) -> add(key.invoke(k), value.invoke(v)));
            return this;
        }

        public HashMap<TKey, TValue> build() {
            return map;
        }

        public builder<TKey, TValue> copy() {
            return new builder<TKey, TValue>().add(map);
        }
    }

    public static <TKey, TValue> builder<TKey, TValue> of(Class<TKey> tKey, Class<TValue> tValue) {
        return new builder<>();
    }

    public static <TKey, TValue> builder<TKey, TValue> of() {
        return new builder<>();
    }

    public static <TKey, TValue> builder<TKey, TValue> of(TKey key, TValue value) {
        return map.<TKey, TValue>of().add(key, value);
    }

    public static <TKey, TValue> builder<TKey, TValue> of(Class<TKey> tKey, Class<TValue> tValue, boolean linked) {
        return new builder<>(linked);
    }

    public static <TKey, TValue> builder<TKey, TValue> of(boolean linked) {
        return new builder<>(linked);
    }

    public static <TKey, TValue> builder<TKey, TValue> of(TKey key, TValue value, boolean linked) {
        return map.<TKey, TValue>of(linked).add(key, value);
    }

    public static <TKey, TValue> builder<TKey, TValue> of(HashMap<TKey, TValue> value) {
        return new builder<>(value);
    }
}
