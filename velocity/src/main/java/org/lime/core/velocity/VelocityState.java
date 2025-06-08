package org.lime.core.velocity;

import org.lime.core.common.api.BaseState;

public interface VelocityState extends BaseState, VelocityIdentity, VelocityServer {
    @Override
    default boolean isEnabled() {
        return server().getPluginManager().isLoaded(pluginId());
    }
}
