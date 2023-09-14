package org.lime.system.toast;

import org.lime.system.execute.*;

// Generated by JavaScript (c) Lime
public class Toast3<T0, T1, T2> extends IToast {
    public LockToast3<T0, T1, T2> lock() { return new LockToast3<>(this); }
    @Override public Object[] getValues() { return new Object[] { val0, val1, val2 }; }
    public Toast3(T0 val0, T1 val1, T2 val2) { this.val0 = val0; this.val1 = val1; this.val2 = val2; }
    public T0 val0; public T1 val1; public T2 val2;
    public T0 get0() { return val0; } public T1 get1() { return val1; } public T2 get2() { return val2; }
    @Override public int size() { return 3; }
    @Override public int hashCode() { return super.hashCode(); }
    @Override public boolean equals(Object obj) { return super.equals(obj); }
    @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; } return null; }
    @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; } }
    @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
    public <A0, A1, A2>Toast3<A0, A1, A2> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2) { return Toast.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2)); }
    public void invoke(Action3<T0, T1, T2> action) { action.invoke(val0, val1, val2); }
    public <T>T invokeGet(Func3<T0, T1, T2, T> func) { return func.invoke(val0, val1, val2); }
}
