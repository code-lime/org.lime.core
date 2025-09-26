package org.lime.core.common.api;

import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;

public interface ConfigAccess<T> {
    T value();
    void save(T value);

    long version();
    Disposable listenUpdating(Action1<T> callback);
}
