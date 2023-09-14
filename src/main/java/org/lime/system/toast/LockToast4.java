package org.lime.system.toast;

import org.lime.system.execute.*;

// Generated by JavaScript (c) Lime
public class LockToast4<T0, T1, T2, T3> extends ILockToast<Toast4<T0, T1, T2, T3>> {
    public LockToast4(Toast4<T0, T1, T2, T3> base) { super(base); }
    public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); }
    public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); }
    public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); }
}