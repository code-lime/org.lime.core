package org.lime.system.execute;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Generated by JavaScript (c) Lime
public interface Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> extends ICallable {
    TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7);
    @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7]); }
    static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> of(Method method) {
        return Modifier.isStatic(method.getModifiers()) ?
                (val0, val1, val2, val3, val4, val5, val6, val7) -> Execute.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }) :
                (val0, val1, val2, val3, val4, val5, val6, val7) -> Execute.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7 });
    }
}