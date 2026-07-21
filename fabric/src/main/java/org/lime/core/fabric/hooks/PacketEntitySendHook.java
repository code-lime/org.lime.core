package org.lime.core.fabric.hooks;

import net.minecraft.server.level.ServerEntity;
import org.jetbrains.annotations.NotNull;

/** Marks a synthetic {@link ServerEntity} whose direct ItemFrame map path is disabled. */
public interface PacketEntitySendHook {
    static void mark(@NotNull ServerEntity entity) {
        ((PacketEntitySendHook)entity).lime$markPacketEntity();
    }

    void lime$markPacketEntity();
}
