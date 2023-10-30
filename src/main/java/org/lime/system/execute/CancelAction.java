package org.lime.system.execute;

@SuppressWarnings("all")
public interface CancelAction extends Action0 {
    default void cancel() {
        invoke();
    }
}
