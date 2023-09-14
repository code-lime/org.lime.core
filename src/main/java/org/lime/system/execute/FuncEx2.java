package org.lime.system.execute;

import java.util.Optional;

// Generated by JavaScript (c) Lime
public interface FuncEx2<T0, T1, TResult> {
    TResult invoke(T0 arg0, T1 arg1) throws Throwable;
    default Func2<T0, T1, TResult> throwable() {
        return (val0, val1) -> { try { return this.invoke(val0, val1); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func2<T0, T1, Optional<TResult>> optional() {
        return (val0, val1) -> { try { return Optional.ofNullable(this.invoke(val0, val1)); } catch (Throwable e) { return Optional.empty(); } };
    }
}