package org.lime.reflection.lambda;

import org.lime.reflection.Reflection;
import org.lime.reflection.ReflectionMethod;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

public class LambdaCreatorProxy implements LambdaCreator {
    private static final Method toStringMethod = ReflectionMethod.of(Object.class, "toString").method();

    private static Object invokeDynamic(MethodHandle handle, @Nullable Object[] args) throws Throwable {
        return switch (args == null ? 0 : args.length) {
            //<editor-fold desc="Invokes">
            //<generator name="invoke-dynamic.js:getAllCases(15)">
            case 0 -> handle.invoke();
            case 1 -> handle.invoke(args[0]);
            case 2 -> handle.invoke(args[0], args[1]);
            case 3 -> handle.invoke(args[0], args[1], args[2]);
            case 4 -> handle.invoke(args[0], args[1], args[2], args[3]);
            case 5 -> handle.invoke(args[0], args[1], args[2], args[3], args[4]);
            case 6 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            case 10 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
            case 11 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]);
            case 12 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11]);
            case 13 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12]);
            case 14 -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13]);
            //</generator>
            //</editor-fold>
            default -> handle.invokeWithArguments(args);
        };
    }

    @Override
    public <T, J extends Executable> T createExecutable(J executable, Class<T> tClass, Method invoke) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(tClass, Reflection.lookup(tClass));

            Reflection.access(executable);

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
                            return invokeDynamic(handle, args);
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public <T> T createField(Field field, boolean isGetter, Class<T> tClass, Method invoke) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(tClass, Reflection.lookup(tClass));

            Reflection.access(field);

            MethodHandle handle = isGetter
                    ? lookup.unreflectGetter(field)
                    : lookup.unreflectSetter(field);
            return tClass.cast(Proxy.newProxyInstance(
                    tClass.getClassLoader(),
                    new Class<?>[]{tClass}, (proxy, method, args) -> {
                        if (method.equals(toStringMethod))
                            return "Lambda of field: " + field;
                        if (method.equals(invoke))
                            return invokeDynamic(handle, args);
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
