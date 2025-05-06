package org.lime.core.paper;

import org.lime.core.common.BaseCoreCommandAccess;

public interface PaperCommandAccess
        extends BaseCoreCommandAccess<CoreCommand.Register, CoreInstancePlugin.CoreInstance> {
    @Override
    default void flushCommands() {
    }
}
