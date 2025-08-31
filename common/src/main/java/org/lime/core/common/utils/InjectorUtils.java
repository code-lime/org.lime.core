package org.lime.core.common.utils;

import com.google.inject.Injector;

import java.util.function.Supplier;

public class InjectorUtils {
    public static <T>T inject(
            Injector injector,
            Supplier<T> factory) {
        T value = factory.get();
        injector.injectMembers(value);
        return value;
    }
}
