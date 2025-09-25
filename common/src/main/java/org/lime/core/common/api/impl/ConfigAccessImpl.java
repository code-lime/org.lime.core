package org.lime.core.common.api.impl;

import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.system.Lazy;
import org.lime.core.common.api.ConfigAccess;
import org.lime.core.common.utils.system.execute.Action1;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ConfigAccessImpl<T>
        implements ConfigAccess<T> {
    private final AtomicLong version = new AtomicLong(0);
    private final boolean updated;
    private final ConcurrentLinkedQueue<Action1<T>> listenUpdating = new ConcurrentLinkedQueue<>();
    protected Lazy<T> access;

    public ConfigAccessImpl(boolean updated) {
        this.updated = updated;
        this.access = Lazy.of(this.read());
        onUpdated();
    }

    @Override
    public T value() {
        return access.value();
    }

    @Override
    public void save(T value) {
        this.access = Lazy.of(value);
        write(value);
        onUpdated();
    }

    public boolean update() {
        if (!updated)
            return false;
        access = Lazy.of(this::read);
        onUpdated();
        return true;
    }

    private void onUpdated() {
        version.incrementAndGet();
        if (listenUpdating.isEmpty())
            return;
        T value = value();
        listenUpdating.forEach(callback -> callback.invoke(value));
    }

    @Override
    public long version() {
        return version.get();
    }
    @Override
    public Disposable listenUpdating(Action1<T> callback) {
        listenUpdating.add(callback);
        callback.invoke(value());
        return () -> listenUpdating.remove(callback);
    }

    protected abstract T read();
    protected abstract void write(T value);
}
