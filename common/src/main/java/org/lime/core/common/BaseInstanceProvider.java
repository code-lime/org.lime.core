package org.lime.core.common;

import com.google.inject.Provider;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.execute.Func1;

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
        public abstract Stream<? extends T> getOwners();
        public abstract <Owner extends T>Owner getOwner(Class<Owner> instanceClass);
        public <Owner>Owner getCastOwner(Class<Owner> ownerClass) {
            if (!ownerBaseClass().isAssignableFrom(ownerClass))
                throw new RuntimeException("Owner "+ownerClass+" not "+ ownerBaseClass());
            return ownerClass.cast(getOwner((Class<T>)ownerClass));
        }

        public static <T>Storage<T> of(
                Class<T> ownerBaseClass,
                Func0<Stream<T>> owners) {
            return new Storage<>() {
                @Override
                protected Class<T> ownerBaseClass() {
                    return ownerBaseClass;
                }
                @Override
                public Stream<? extends T> getOwners() {
                    return owners.invoke();
                }
                @Override
                public <J extends T> J getOwner(Class<J> ownerClass) {
                    return owners.invoke()
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
