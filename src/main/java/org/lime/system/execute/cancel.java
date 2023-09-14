package org.lime.system.execute;

@SuppressWarnings("all")
public interface cancel extends Action0 {
    default void cancel() {
        invoke();
    }
}
