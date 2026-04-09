package org.lime.core.fabric.services;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.memories.BaseConnectionStorageService;
import org.lime.core.common.utils.Disposable;

import java.util.UUID;
import java.util.stream.Stream;

@BindService
public class ConnectionStorageService
        extends BaseConnectionStorageService {
    @Inject PlayerList playerList;

    @Override
    public Disposable register() {
        ServerPlayConnectionEvents.INIT.register((handler, server) -> handleLogin(handler.getPlayer().getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> handleLogout(handler.getPlayer().getUUID()));
        return super.register();
    }

    @Override
    protected Stream<UUID> onlinePlayerIds() {
        return playerList.getPlayers().stream().map(Entity::getUUID);
    }
}
