package org.lime.core.common;

import com.google.inject.Provider;
import org.lime.core.common.utils.system.Lazy;
import org.lime.core.common.utils.system.execute.Func0;
import org.lime.core.common.utils.system.execute.Func1;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public abstract class BaseInstanceProvider<Owner> {
    private static final AtomicReference<Storage<?>> storage = new AtomicReference<>(null);
    protected static void setStorage(Storage<?> storage) {
        var result = BaseInstanceProvider.storage.updateAndGet(current -> current == null ? storage : current);
        if (result == storage)
            return;
        throw new RuntimeException("Storage already configured: " + result);
    }

    private final Lazy<Owner> lazy = Lazy.of(() -> storage.get().getCastOwner(ownerClass()));

    protected abstract Class<Owner> ownerClass();
    protected abstract BaseInstance<?> instance(Owner owner);

    public Owner get() {
        return lazy.value();
    }
    public <T>Provider<T> provider(Class<T> type) {
        final Lazy<Provider<T>> otherProvider = Lazy.of(() -> instance(get()).injector().getProvider(type));
        return () -> otherProvider.value().get();
    }

    protected static abstract class Storage<T> {
        protected abstract Class<T> ownerBaseClass();
        public abstract <Owner extends T>Owner getOwner(Class<Owner> instanceClass);
        public <Owner>Owner getCastOwner(Class<Owner> ownerClass) {
            if (!ownerBaseClass().isAssignableFrom(ownerClass))
                throw new RuntimeException("Owner "+ownerClass+" not "+ ownerBaseClass());
            return ownerClass.cast(getOwner((Class<T>)ownerClass));
        }

        public static <T>Storage<T> of(
                Class<T> ownerBaseClass,
                Func1<Class<? extends T>, Optional<T>> finder) {
            return new Storage<>() {
                @Override
                protected Class<T> ownerBaseClass() {
                    return ownerBaseClass;
                }
                @Override
                public <J extends T> J getOwner(Class<J> ownerClass) {
                    return finder.invoke(ownerClass)
                            .filter(ownerClass::isInstance)
                            .map(ownerClass::cast)
                            .orElseThrow(() -> new IllegalArgumentException("Owner of "+ownerClass+" not found"));
                }
            };
        }
        public static <T>Storage<T> of(
                Class<T> ownerBaseClass,
                Func0<Stream<T>> finder) {
            return new Storage<>() {
                @Override
                protected Class<T> ownerBaseClass() {
                    return ownerBaseClass;
                }
                @Override
                public <J extends T> J getOwner(Class<J> ownerClass) {
                    return finder.invoke()
                            .filter(ownerClass::isInstance)
                            .map(ownerClass::cast)
                            .filter(v -> v.getClass() == ownerClass)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Owner of "+ownerClass+" not found"));
                }
            };
        }
    }
}
