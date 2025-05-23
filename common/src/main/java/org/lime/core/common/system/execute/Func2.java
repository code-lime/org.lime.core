package org.lime.core.common.system.execute;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Generated by JavaScript (c) Lime
public interface Func2<T0, T1, TResult> extends Callable {
    TResult invoke(T0 arg0, T1 arg1);
    @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1]); }
    static <T0, T1, TResult>Func2<T0, T1, TResult> of(Method method) {
        return Modifier.isStatic(method.getModifiers()) ?
                (val0, val1) -> Execute.invoke(method, null, new Object[] { val0, val1 }) :
                (val0, val1) -> Execute.invoke(method, val0, new Object[] { val1 });
    }
}