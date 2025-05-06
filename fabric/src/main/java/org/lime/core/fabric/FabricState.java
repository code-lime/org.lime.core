package org.lime.core.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.lime.core.common.api.BaseState;

public interface FabricState extends BaseState, FabricIdentity {
    @Override
    default boolean isEnabled() {
        return FabricLoader.getInstance().isModLoaded(modId());
    }
}
