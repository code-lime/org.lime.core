package org.lime.core.common.system.execute;

import java.util.Optional;

// Generated by JavaScript (c) Lime
public interface FuncEx3<T0, T1, T2, TResult> extends Callable {
    TResult invoke(T0 arg0, T1 arg1, T2 arg2) throws Throwable;
    default Func3<T0, T1, T2, TResult> throwable() {
        return (val0, val1, val2) -> { try { return this.invoke(val0, val1, val2); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func3<T0, T1, T2, Optional<TResult>> optional() {
        return (val0, val1, val2) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2)); } catch (Throwable e) { return Optional.empty(); } };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}