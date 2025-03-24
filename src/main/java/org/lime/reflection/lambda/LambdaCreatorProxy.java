package org.lime.reflection.lambda;

import org.lime.reflection.ReflectionMethod;
import org.lime.reflection.TestNative;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

public class LambdaCreatorProxy implements LambdaCreator {
    private static final Method toStringMethod = ReflectionMethod.of(Object.class, "toString").method();

    @Override
    public <T, J extends Executable> T createExecutable(J executable, Class<T> tClass, Method invoke) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(tClass, TestNative.lookup(tClass));
            MethodHandle handle;

            if (executable instanceof Method method) handle = lookup.unreflect(method);
            else if (executable instanceof Constructor<?> constructor) handle = lookup.unreflectConstructor(constructor);
            else throw new IllegalArgumentException("Unsupported member type: " + executable.getClass());

            return tClass.cast(Proxy.newProxyInstance(
                    tClass.getClassLoader(),
                    new Class<?>[]{tClass}, (proxy, method, args) -> {
                        if (method.equals(toStringMethod))
                            return "Lambda of method: " + executable;
                        if (method.equals(invoke))
                            return handle.invoke(args);
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public <T> T createField(Field field, boolean isGetter, Class<T> tClass, Method invoke) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(tClass, TestNative.lookup(tClass));
            MethodHandle handle = isGetter
                    ? lookup.unreflectGetter(field)
                    : lookup.unreflectSetter(field);
            return tClass.cast(Proxy.newProxyInstance(
                    tClass.getClassLoader(),
                    new Class<?>[]{tClass}, (proxy, method, args) -> {
                        if (method.equals(toStringMethod))
                            return "Lambda of field: " + field;
                        if (method.equals(invoke))
                            return handle.invoke(args);
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
