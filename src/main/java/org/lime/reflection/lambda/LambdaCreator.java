package org.lime.reflection.lambda;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface LambdaCreator {
    <T, J extends Executable>T createExecutable(J executable, Class<T> tClass, Method invoke);
    <T>T createField(Field field, boolean isGetter, Class<T> tClass, Method invoke);
}
