package org.lime.system.execute;

public interface ActionEx0 {
    void invoke() throws Throwable;
    default Action0 throwable() {
        return () -> { try { this.invoke(); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
    }
    default Func0<Boolean> optional() {
        return () -> { try { this.invoke(); return true; } catch (Throwable e) { return false; } };
    }
}
