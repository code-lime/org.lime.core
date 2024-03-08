package org.lime.system.execute;

import java.util.Optional;

// Generated by JavaScript (c) Lime
public interface FuncEx4<T0, T1, T2, T3, TResult> extends ICallable {
    TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Throwable;
    default Func4<T0, T1, T2, T3, TResult> throwable() {
        return (val0, val1, val2, val3) -> { try { return this.invoke(val0, val1, val2, val3); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func4<T0, T1, T2, T3, Optional<TResult>> optional() {
        return (val0, val1, val2, val3) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3)); } catch (Throwable e) { return Optional.empty(); } };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}