package org.lime.core.paper.services;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.bungee.BaseBungeeApi;
import org.lime.core.common.utils.Disposable;

@BindService
public class BungeeApi
        extends BaseBungeeApi<Player> {
    @Inject Plugin plugin;
    @Inject Messenger messenger;

    @Override
    public Disposable register() {
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL_ID);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL_ID, this::onPluginMessageReceived);
        return super.register();
    }
    @Override
    public void unregister() {
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL_ID, this::onPluginMessageReceived);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL_ID);
        super.unregister();
    }

    @Override
    protected void sendBungeeMessage(Player player, ByteArrayDataOutput output) {
        player.sendPluginMessage(plugin, CHANNEL_ID, output.toByteArray());
    }
    @Override
    protected Player getFirstPlayer() {
        var player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player == null)
            throw new IllegalArgumentException("Bungee Messaging Api requires at least one player to be online.");
        return player;
    }

    private void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        onPluginMessageReceived(player, ByteStreams.newDataInput(message));
    }
}
