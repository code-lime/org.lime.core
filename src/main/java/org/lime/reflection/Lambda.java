package org.lime.reflection;

import org.lime.reflection.lambda.LambdaCreator;
import org.lime.reflection.lambda.LambdaCreatorProxy;
import org.lime.system.execute.Execute;
import org.lime.system.execute.ICallable;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple3;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Lambda {
    private static final ConcurrentHashMap<Tuple3<Class<?>, Member, Object>, Object> cachedCallable = new ConcurrentHashMap<>();

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

    public static ICallable lambda(Method method) {
        return lambda(method, Execute.findClass(getExecuteCount(method), method.getReturnType() != void.class, false));
    }
    public static <T>T lambda(Method method, Class<T> tClass) {
        return lambda(method, tClass, getInvokeMethod(tClass));
    }
    public static <T>T lambda(Method method, Class<T> tClass, Method invoke) {
        return createLambda(method, tClass, invoke);
    }

    public static ICallable lambda(Constructor<?> constructor) {
        return lambda(constructor, Execute.findClass(constructor.getParameterCount(), true, false));
    }
    public static <T>T lambda(Constructor<?> constructor, Class<T> tClass) {
        return lambda(constructor, tClass, getInvokeMethod(tClass));
    }
    public static <T>T lambda(Constructor<?> constructor, Class<T> tClass, Method invoke) {
        return createLambda(constructor, tClass, invoke);
    }

    public static ICallable getter(Field field) {
        return getter(field, Execute.findClass(Modifier.isStatic(field.getModifiers()) ? 0 : 1, true, false));
    }
    public static <T>T getter(Field field, Class<T> tClass) {
        return getter(field, tClass, getInvokeMethod(tClass));
    }
    public static <T>T getter(Field field, Class<T> tClass, Method invoke) {
        return createField(field, true, tClass, invoke);
    }

    public static ICallable setter(Field field) {
        return setter(field, Execute.findClass(1 + (Modifier.isStatic(field.getModifiers()) ? 0 : 1), false, false));
    }
    public static <T>T setter(Field field, Class<T> tClass) {
        return setter(field, tClass, getInvokeMethod(tClass));
    }
    public static <T>T setter(Field field, Class<T> tClass, Method invoke) {
        return createField(field, false, tClass, invoke);
    }

    private static final LambdaCreator creator = new LambdaCreatorProxy();

    private static <T, J extends Executable>T createLambda(
            J executable,
            Class<T> tClass,
            Method invoke) {
        return (T) cachedCallable.computeIfAbsent(Tuple.of(tClass, executable, null), _ -> createExecutableLambda(executable, tClass, invoke));
    }
    private static <T, J extends Executable>T createExecutableLambda(
            J executable,
            Class<T> tClass,
            Method invoke) {
        return creator.createExecutable(executable, tClass, invoke);
    }
    private static <T>T createField(
            Field field,
            boolean isGetter,
            Class<T> tClass,
            Method invoke) {
        return (T) cachedCallable.computeIfAbsent(Tuple.of(tClass, field, isGetter), _ -> createFieldLambda(field, isGetter, tClass, invoke));
    }
    private static <T>T createFieldLambda(
            Field field,
            boolean isGetter,
            Class<T> tClass,
            Method invoke) {
        return creator.createField(field, isGetter, tClass, invoke);
    }
}
