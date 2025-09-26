package org.lime.core.common.api.scope;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.Service;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action0;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Execute;
import org.lime.core.common.utils.execute.Func0;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("resource")
public abstract class BaseKeyedScope<Instance extends BaseInstance<Instance>, TKey>
        implements Scope, Disposable {
    protected record Data<Instance extends BaseInstance<Instance>, TKey>(
            TKey key,
            BaseKeyedScope<Instance, TKey> scope,
            Map<Key<?>, Object> elements,
            Disposable.Composite disposable)
            implements Disposable {
        @SuppressWarnings("unchecked")
        public <T> T accessElement(Key<T> elementKey, Provider<T> unscoped) {
            AtomicBoolean register = new AtomicBoolean(false);
            T element = (T)elements.computeIfAbsent(elementKey, v -> {
                register.set(true);
                return unscoped.get();
            });
            if (register.get())
                this.disposable.add(scope.register(elementKey, element));
            return element;
        }
        @Override
        public void close() {
            disposable.close();
            elements.clear();
        }
    }

    protected final Instance instance;
    protected final Map<TKey, Data<Instance, TKey>> sessions = new ConcurrentHashMap<>();
    protected final AtomicReference<TKey> current = new AtomicReference<>();

    protected BaseKeyedScope(Instance instance) {
        this.instance = instance;
    }

    public void enter(TKey key) {
        Objects.requireNonNull(key, "key is null");
        if (!current.compareAndSet(null, key))
            throw new OutOfScopeException("Another key is active (exit not called)");
        accessData(key);
    }
    public void exit(TKey key) {
        Objects.requireNonNull(key, "key is null");
        if (!current.compareAndSet(key, null))
            throw new OutOfScopeException("Key already exit or another key is active");
    }
    public Action0 use(TKey key) {
        enter(key);
        return () -> exit(key);
    }
    public boolean existUse(TKey key, Action0 execute) {
        Objects.requireNonNull(key, "key is null");
        if (!sessions.containsKey(key))
            return false;
        try (var ignored = use(key)) {
            execute.invoke();
        }
        return true;
    }
    public <T>Optional<T> existUse(TKey key, Func0<T> execute) {
        Objects.requireNonNull(key, "key is null");
        if (!sessions.containsKey(key))
            return Optional.empty();
        try (var ignored = use(key)) {
            return Optional.ofNullable(execute.invoke());
        }
    }

    public void sync(Iterable<TKey> key) {
        sync(key, (Action0)null);
    }
    public void sync(Iterable<TKey> key, @Nullable Action0 execute) {
        Set<TKey> removeKeys = new HashSet<>(sessions.keySet());
        key.forEach(sessionKey -> {
            removeKeys.remove(sessionKey);

            try (var ignored = use(sessionKey)) {
                if (execute != null)
                    execute.invoke();
            }
        });
        removeKeys.forEach(this::destroy);
    }
    public void sync(Iterable<TKey> key, @Nullable Action1<TKey> execute) {
        Set<TKey> removeKeys = new HashSet<>(sessions.keySet());
        key.forEach(sessionKey -> {
            removeKeys.remove(sessionKey);

            try (var ignored = use(sessionKey)) {
                if (execute != null)
                    execute.invoke(sessionKey);
            }
        });
        removeKeys.forEach(this::destroy);
    }

    public TKey current() {
        return current.get();
    }

    public void destroy(TKey key) {
        Objects.requireNonNull(key, "key is null");
        var data = sessions.remove(key);
        if (data == null)
            return;
        data.close();
    }

    protected Data<Instance, TKey> createData(TKey key) {
        return new Data<>(key, this, new ConcurrentHashMap<>(), Disposable.composite());
    }
    protected Data<Instance, TKey> accessData(TKey key) {
        return sessions.computeIfAbsent(key, this::createData);
    }
    protected <T>Disposable.Composite register(Key<T> elementKey, T element) {
        Disposable.Composite composite = Disposable.composite();
        if (element instanceof Closeable closeable) {
            composite.add(Execute.actionEx(closeable::close).throwable()::invoke);
        }
        if (element instanceof Service service) {
            composite.add(service.register());
            composite.add(service::unregister);
        }
        return composite;
    }

    @Override
    public void close() {
        sessions.values().removeIf(data -> {
            data.close();
            return true;
        });
    }
    @Override
    public <T> Provider<T> scope(final Key<T> elementKey, final Provider<T> unscoped) {
        return () -> {
            TKey key = current.get();
            if (key == null)
                throw new OutOfScopeException("No key is entered");
            return accessData(key).accessElement(elementKey, unscoped);
        };
    }
}
