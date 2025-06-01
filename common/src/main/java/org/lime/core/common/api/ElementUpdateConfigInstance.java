package org.lime.core.common.api;

import org.lime.core.common.system.execute.Action0;

import java.util.Collection;

public interface ElementUpdateConfigInstance {
    default void updateConfigSync() { }
    default void updateConfigAsync(Collection<String> files, Action0 updated) {
        updated.invoke();
    }
}
