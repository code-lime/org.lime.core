package org.lime.system.execute;

public interface ActionEx1<T0> extends ICallable {
    void invoke(T0 arg0) throws Throwable;
    default Action1<T0> throwable() {
        return (val0) -> { try { this.invoke(val0); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func1<T0, Boolean> optional() {
        return (val0) -> { try { this.invoke(val0); return true; } catch (Throwable e) { return false; } };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}
