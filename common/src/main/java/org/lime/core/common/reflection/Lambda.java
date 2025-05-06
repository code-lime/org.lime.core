package org.lime.core.common.reflection;

import org.lime.core.common.reflection.lambda.LambdaCreator;
import org.lime.core.common.reflection.lambda.LambdaCreatorProxy;
import org.lime.core.common.system.execute.Execute;
import org.lime.core.common.system.execute.Callable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;

public class Lambda {
    private static final LambdaCreator creator = new LambdaCreatorProxy().cache();

    private static int getExecuteCount(Executable executable) {
        return executable.getParameterCount() + (Modifier.isStatic(executable.getModifiers()) ? 0 : 1);
    }
    private static Method getInvokeMethod(Class<?> tClass) {
        List<Method> samMethods = Arrays.stream(tClass.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .toList();
        if (samMethods.size() != 1)
            throw new IllegalArgumentException("Interface " + tClass + " not functional");
        return samMethods.getFirst();
    }

    public static Callable lambda(Method method) {
        return lambda(method, Execute.findClass(getExecuteCount(method), method.getReturnType() != void.class, false));
    }
    public static <T>T lambda(Method method, Class<T> tClass) {
        return lambda(method, tClass, getInvokeMethod(tClass));
    }
    public static <T>T lambda(Method method, Class<T> tClass, Method invoke) {
        return creator.executable(method, tClass, invoke);
    }

    public static Callable lambda(Constructor<?> constructor) {
        return lambda(constructor, Execute.findClass(constructor.getParameterCount(), true, false));
    }
    public static <T>T lambda(Constructor<?> constructor, Class<T> tClass) {
        return lambda(constructor, tClass, getInvokeMethod(tClass));
    }
    public static <T>T lambda(Constructor<?> constructor, Class<T> tClass, Method invoke) {
        return creator.executable(constructor, tClass, invoke);
    }

    public static Callable getter(Field field) {
        return getter(field, Execute.findClass(Modifier.isStatic(field.getModifiers()) ? 0 : 1, true, false));
    }
    public static <T>T getter(Field field, Class<T> tClass) {
        return getter(field, tClass, getInvokeMethod(tClass));
    }
    public static <T>T getter(Field field, Class<T> tClass, Method invoke) {
        return creator.field(field, true, tClass, invoke);
    }

    public static Callable setter(Field field) {
        return setter(field, Execute.findClass(1 + (Modifier.isStatic(field.getModifiers()) ? 0 : 1), false, false));
    }
    public static <T>T setter(Field field, Class<T> tClass) {
        return setter(field, tClass, getInvokeMethod(tClass));
    }
    public static <T>T setter(Field field, Class<T> tClass, Method invoke) {
        return creator.field(field, false, tClass, invoke);
    }
}
