package org.lime.core.velocity;

import org.lime.core.common.BaseCoreCommandAccess;

public interface VelocityCommandAccess
        extends BaseCoreCommandAccess<CoreCommand.Register, CoreInstance>, VelocityServer {
    @Override
    default void flushCommands() {
    }
}
