package org.lime.core.common.utils;

import org.lime.core.common.utils.system.Lock;
import org.lime.core.common.utils.system.execute.Func1;

import java.util.Objects;

public class KeyedProvider<K, V> {
    private final Lock lock = Lock.create();

    private K key;
    private V value;

    private final Func1<K, V> factory;

    public KeyedProvider(K initialKey, Func1<K, V> factory) {
        this.factory = factory;
        this.key = initialKey;
        this.value = factory.apply(initialKey);
    }

    public V get(K key) {
        try (var ignored = lock.lock()) {
            if (!Objects.equals(this.key, key)) {
                this.key = key;
                this.value = factory.apply(key);
            }
            return this.value;
        }
    }

    public static <K, V>KeyedProvider<K, V> of(K initialKey, Func1<K, V> factory) {
        return new KeyedProvider<>(initialKey, factory);
    }
}
