package org.lime.system.toast;

import org.lime.system.execute.*;

// Generated by JavaScript (c) Lime
public class LockToast1<T0> extends ILockToast<Toast1<T0>> {
    public LockToast1(Toast1<T0> base) { super(base); }
    public T0 get0() { return (T0)get(0); }
    public void set0(T0 value) { set(0, value); }
    public T0 edit0(Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); }
}