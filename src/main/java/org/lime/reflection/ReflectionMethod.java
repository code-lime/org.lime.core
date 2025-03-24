package org.lime.reflection;

import org.lime.system.execute.ICallable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public record ReflectionMethod(Method method) {
    public static ReflectionMethod of(Method method) {
        return new ReflectionMethod(method);
    }

    public static ReflectionMethod of(Class<?> tClass, String name, Class<?>... args) {
        return of(Reflection.get(tClass, name, args));
    }

    public static String methodToString(Method method, boolean mojang) {
        return methodToString(method.getDeclaringClass(), mojang ? Reflection.name(method) : method.getName(), method.getParameterTypes());
    }

    public static String methodToString(Class<?> tClass, String name, Class<?>[] argTypes) {
        return tClass.getName() + '.' + name + ((argTypes == null || argTypes.length == 0) ? "()" : Arrays.stream(argTypes)
                .map(c -> c == null ? "null" : c.getName())
                .collect(Collectors.joining(",", "(", ")")));
    }

    public static ReflectionMethod ofMojang(Class<?> tClass, String mojang_name, Class<?>... args) {
        return of(Reflection.getFirst(tClass, m -> Reflection.name(m).equals(mojang_name), args)
                .orElseThrow(() -> new IllegalArgumentException(new NoSuchMethodException(methodToString(tClass, mojang_name, args)))));
    }

    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    public Object call(Object instance, Object[] args) {
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Object call(Object[] args) {
        return isStatic()
                ? call(null, args)
                : call(args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public ICallable lambda() {
        return Lambda.lambda(method);
    }
    public <J> J lambda(Class<J> tClass) {
        return Lambda.lambda(method, tClass);
    }
    public <J> J lambda(Class<J> tClass, Method invoke) {
        return Lambda.lambda(method, tClass, invoke);
    }
}
