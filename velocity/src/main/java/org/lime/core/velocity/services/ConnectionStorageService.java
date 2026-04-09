package org.lime.core.velocity.services;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.memories.BaseConnectionStorageService;

import java.util.UUID;
import java.util.stream.Stream;

@BindService
public class ConnectionStorageService
        extends BaseConnectionStorageService {
    @Inject ProxyServer server;

    @Override
    protected Stream<UUID> onlinePlayerIds() {
        return server.getAllPlayers().stream().map(Player::getUniqueId);
    }

    @Subscribe
    private void on(LoginEvent event) {
        handleLogin(event.getPlayer().getUniqueId());
    }
    @Subscribe
    private void on(DisconnectEvent event) {
        handleLogin(event.getPlayer().getUniqueId());
    }
}
