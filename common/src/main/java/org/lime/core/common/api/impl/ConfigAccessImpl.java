package org.lime.core.common.api.impl;

import org.lime.core.common.utils.system.Lazy;
import org.lime.core.common.api.ConfigAccess;

public abstract class ConfigAccessImpl<T>
        implements ConfigAccess<T> {
    private final boolean updated;
    protected Lazy<T> access;

    public ConfigAccessImpl(boolean updated) {
        this.updated = updated;
        this.access = Lazy.of(this.read());
    }

    @Override
    public T value() {
        return access.value();
    }

    @Override
    public void save(T value) {
        this.access = Lazy.of(value);
        write(value);
    }

    public boolean update() {
        if (!updated)
            return false;
        access = Lazy.of(this::read);
        return true;
    }

    protected abstract T read();
    protected abstract void write(T value);
}
