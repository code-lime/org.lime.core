package org.lime.core.common.reflection.lambda;

import org.lime.core.common.system.tuple.Tuple;
import org.lime.core.common.system.tuple.Tuple3;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class LambdaCreatorCache implements LambdaCreator {
    private final LambdaCreator other;
    private final ConcurrentHashMap<Tuple3<Class<?>, Member, Object>, Object> cache = new ConcurrentHashMap<>();

    public LambdaCreatorCache(LambdaCreator other) {
        this.other = other;
    }

    @Override
    public <T, J extends Executable> T executable(J executable, Class<T> tClass, Method invoke) {
        return tClass.cast(cache.computeIfAbsent(
                Tuple.of(tClass, executable, null),
                _ -> other.executable(executable, tClass, invoke)));
    }
    @Override
    public <T> T field(Field field, boolean isGetter, Class<T> tClass, Method invoke) {
        return tClass.cast(cache.computeIfAbsent(
                Tuple.of(tClass, field, isGetter),
                _ -> other.field(field, isGetter, tClass, invoke)));
    }
}