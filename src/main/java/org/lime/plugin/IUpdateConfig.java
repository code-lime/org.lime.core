package org.lime.plugin;

import org.lime.system.execute.Action0;

import java.util.Collection;

public interface IUpdateConfig {
    default void updateConfigSync() {
    }

    default void updateConfigAsync(Collection<String> files, Action0 updated) {
    }
}
