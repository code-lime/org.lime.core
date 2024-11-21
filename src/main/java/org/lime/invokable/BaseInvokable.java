package org.lime.invokable;

public abstract class BaseInvokable {
    public int waitTicks;

    public BaseInvokable(int waitTicks) { this.waitTicks = waitTicks; }
    public abstract void invoke() throws Throwable;
    public boolean tryRemoveInvoke() throws Throwable {
        if (--waitTicks > 0) return false;
        invoke();
        return true;
    }
}
