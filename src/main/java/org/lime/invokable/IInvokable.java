package org.lime.invokable;

public abstract class IInvokable {
    public int waitTicks;

    public IInvokable(int waitTicks) { this.waitTicks = waitTicks; }
    public abstract void invoke() throws Throwable;
    public boolean tryRemoveInvoke() throws Throwable {
        if (--waitTicks > 0) return false;
        invoke();
        return true;
    }
}
