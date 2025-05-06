package org.lime.core.common.reflection.lambda;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface LambdaCreator {
    <T, J extends Executable>T executable(J executable, Class<T> tClass, Method invoke);
    <T>T field(Field field, boolean isGetter, Class<T> tClass, Method invoke);

    default LambdaCreator cache() {
        return this instanceof LambdaCreatorCache cache
                ? cache
                : new LambdaCreatorCache(this);
    }
}
