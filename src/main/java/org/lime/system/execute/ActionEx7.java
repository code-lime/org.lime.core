package org.lime.system.execute;

// Generated by JavaScript (c) Lime
public interface ActionEx7<T0, T1, T2, T3, T4, T5, T6> extends ICallable {
    void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) throws Throwable;
    default Action7<T0, T1, T2, T3, T4, T5, T6> throwable() {
        return (val0, val1, val2, val3, val4, val5, val6) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func7<T0, T1, T2, T3, T4, T5, T6, Boolean> optional() {
        return (val0, val1, val2, val3, val4, val5, val6) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6); return true; } catch (Throwable e) { return false; } };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}