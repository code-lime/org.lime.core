package org.lime.core.common.system.execute;

public interface ActionEx0 extends Callable {
    void invoke() throws Throwable;
    default Action0 throwable() {
        return () -> { try { this.invoke(); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func0<Boolean> optional() {
        return () -> { try { this.invoke(); return true; } catch (Throwable e) { return false; } };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}
