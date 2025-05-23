package org.lime.core.common.system.tuple;

import org.lime.core.common.system.execute.*;

// Generated by JavaScript (c) Lime
public class Tuple6<T0, T1, T2, T3, T4, T5> extends BaseTuple {
    public LockTuple6<T0, T1, T2, T3, T4, T5> lock() { return new LockTuple6<>(this); }
    @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5 }; }
    public Tuple6(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; }
    public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5;
    public T0 get0() { return val0; } public T1 get1() { return val1; } public T2 get2() { return val2; } public T3 get3() { return val3; } public T4 get4() { return val4; } public T5 get5() { return val5; }
    @Override public int size() { return 6; }
    @Override public int hashCode() { return super.hashCode(); }
    @Override public boolean equals(Object obj) { return super.equals(obj); }
    @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; } return null; }
    @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; } }
    @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
    public void set(Tuple6<T0, T1, T2, T3, T4, T5> other) { this.val0 = other.val0; this.val1 = other.val1; this.val2 = other.val2; this.val3 = other.val3; this.val4 = other.val4; this.val5 = other.val5; }
    public <A0, A1, A2, A3, A4, A5>Tuple6<A0, A1, A2, A3, A4, A5> map(Func1<T0, A0> map0, Func1<T1, A1> map1, Func1<T2, A2> map2, Func1<T3, A3> map3, Func1<T4, A4> map4, Func1<T5, A5> map5) { return Tuple.of(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5)); }
    public void invoke(Action6<T0, T1, T2, T3, T4, T5> action) { action.invoke(val0, val1, val2, val3, val4, val5); }
    public <T>T invokeGet(Func6<T0, T1, T2, T3, T4, T5, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5); }
}