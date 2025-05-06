package org.lime.core.fabric;

import net.minecraft.server.MinecraftServer;
import org.lime.core.common.BaseCoreCommandAccess;

public interface FabricCommandAccess
        extends BaseCoreCommandAccess<CoreCommand.Register, CoreInstance>, FabricServer {
    @Override
    default void flushCommands() {
        MinecraftServer server = server();
        server.getPlayerList()
                .getPlayers()
                .forEach(server.getCommands()::sendCommands);
    }
}
