package org.lime.core.common.system.execute;

import java.util.Optional;

// Generated by JavaScript (c) Lime
public interface FuncEx5<T0, T1, T2, T3, T4, TResult> extends Callable {
    TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) throws Throwable;
    default Func5<T0, T1, T2, T3, T4, TResult> throwable() {
        return (val0, val1, val2, val3, val4) -> { try { return this.invoke(val0, val1, val2, val3, val4); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func5<T0, T1, T2, T3, T4, Optional<TResult>> optional() {
        return (val0, val1, val2, val3, val4) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4)); } catch (Throwable e) { return Optional.empty(); } };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}