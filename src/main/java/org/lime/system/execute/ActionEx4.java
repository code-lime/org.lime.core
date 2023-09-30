package org.lime.system.execute;

// Generated by JavaScript (c) Lime
public interface ActionEx4<T0, T1, T2, T3> {
    void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Throwable;
    default Action4<T0, T1, T2, T3> throwable() {
        return (val0, val1, val2, val3) -> { try { this.invoke(val0, val1, val2, val3); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func4<T0, T1, T2, T3, Boolean> optional() {
        return (val0, val1, val2, val3) -> { try { this.invoke(val0, val1, val2, val3); return true; } catch (Throwable e) { return false; } };
    }
}