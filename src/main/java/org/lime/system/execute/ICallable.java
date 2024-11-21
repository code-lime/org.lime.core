package org.lime.system.execute;

import java.io.Serializable;

public interface ICallable extends Serializable {
    Object call(Object[] args);

    default Object createObjectProxy(Class<?> tClass, String method) {
        return createObjectProxy(tClass, method, this);
    }

    default <T> T createProxy(Class<T> tClass, String method) {
        return createProxy(tClass, method, this);
    }

    static <T> T createProxy(Class<T> tClass, String method, ICallable executor) {
        return (T) createObjectProxy(tClass, method, executor);
    }

    static Object createObjectProxy(Class<?> tClass, String method, ICallable executor) {
        return java.lang.reflect.Proxy.newProxyInstance(
                tClass.getClassLoader(),
                new Class[]{tClass},
                (proxy, method1, args) -> {
                    String method_name = method1.getName();
                    if (!method_name.equals(method)) return false;
                    return executor.call(args);
                });
    }
}
