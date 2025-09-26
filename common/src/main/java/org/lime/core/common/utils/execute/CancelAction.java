package org.lime.core.common.utils.execute;

@SuppressWarnings("all")
public interface CancelAction extends Action0 {
    default void cancel() {
        invoke();
    }
}
