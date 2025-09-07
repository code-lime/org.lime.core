package org.lime.core.common;

import com.google.inject.Provider;
import org.lime.core.common.utils.system.Lazy;
import org.lime.core.common.utils.system.execute.Func0;
import org.lime.core.common.utils.system.execute.Func1;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public abstract class BaseInstanceProvider<Instance extends BaseInstance<?>> {
    private static final AtomicReference<Storage<?>> storage = new AtomicReference<>(null);
    protected static void setStorage(Storage<?> storage) {
        var result = BaseInstanceProvider.storage.updateAndGet(current -> current == null ? storage : current);
        if (result == storage)
            return;
        throw new RuntimeException("Storage already configured: " + result);
    }

    private final Lazy<Instance> lazy = Lazy.of(() -> storage.get().getCastInstance(instanceClass()));

    protected abstract Class<Instance> instanceClass();

    public Instance get() {
        return lazy.value();
    }
    public <T>Provider<T> provider(Class<T> type) {
        final Lazy<Provider<T>> otherProvider = Lazy.of(() -> get().injector().getProvider(type));
        return () -> otherProvider.value().get();
    }

    protected static abstract class Storage<T extends BaseInstance<T>> {
        protected abstract Class<T> instanceBaseClass();
        public abstract <Instance extends T>Instance getInstance(Class<Instance> instanceClass);
        public <Instance extends BaseInstance<?>>Instance getCastInstance(Class<Instance> instanceClass) {
            if (!instanceBaseClass().isAssignableFrom(instanceClass))
                throw new RuntimeException("Instance "+instanceClass+" not "+instanceBaseClass());
            return instanceClass.cast(getInstance((Class<T>)instanceClass));
        }

        public static <T extends BaseInstance<T>>Storage<T> of(
                Class<T> instanceBaseClass,
                Func1<Class<? extends T>, Optional<T>> finder) {
            return new Storage<>() {
                @Override
                protected Class<T> instanceBaseClass() {
                    return instanceBaseClass;
                }
                @Override
                public <J extends T> J getInstance(Class<J> instanceClass) {
                    return finder.invoke(instanceClass)
                            .filter(instanceClass::isInstance)
                            .map(instanceClass::cast)
                            .orElseThrow(() -> new IllegalArgumentException("Instance of "+instanceClass+" not found"));
                }
            };
        }
        public static <T extends BaseInstance<T>>Storage<T> of(
                Class<T> instanceBaseClass,
                Func0<Stream<Object>> finder) {
            return new Storage<>() {
                @Override
                protected Class<T> instanceBaseClass() {
                    return instanceBaseClass;
                }
                @Override
                public <J extends T> J getInstance(Class<J> instanceClass) {
                    return finder.invoke()
                            .filter(instanceClass::isInstance)
                            .map(instanceClass::cast)
                            .filter(v -> v.getClass() == instanceClass)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Instance of "+instanceClass+" not found"));
                }
            };
        }
    }
}
