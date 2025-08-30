package org.lime.core.common.api;

import org.lime.core.common.utils.Disposable;

public interface Service {
    default Disposable register() {
        return Disposable.empty();
    }
    default void unregister() {}
}
