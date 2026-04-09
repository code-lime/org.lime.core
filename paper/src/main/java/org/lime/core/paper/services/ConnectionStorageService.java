package org.lime.core.paper.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.memories.BaseConnectionStorageService;

import java.util.UUID;
import java.util.stream.Stream;

@BindService
public class ConnectionStorageService
        extends BaseConnectionStorageService
        implements Listener {
    @Override
    protected Stream<UUID> onlinePlayerIds() {
        return Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId);
    }

    @EventHandler
    private void on(PlayerLoginEvent event) {
        handleLogin(event.getPlayer().getUniqueId());
    }
    @EventHandler
    private void on(PlayerQuitEvent event) {
        handleLogout(event.getPlayer().getUniqueId());
    }
}
