package org.lime.core.common.utils.system.execute;

@SuppressWarnings("all")
public interface CancelAction extends Action0 {
    default void cancel() {
        invoke();
    }
}
