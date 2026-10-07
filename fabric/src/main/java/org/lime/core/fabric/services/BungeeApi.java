package org.lime.core.fabric.services;

import com.google.common.io.*;
import com.google.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
//#switch PROPERTIES.versionMinecraft
//#caseof 1.20.1
import io.netty.buffer.Unpooled;
//#default
//OF//import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//OF//import net.minecraft.network.codec.StreamCodec;
//OF//import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#endswitch
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.bungee.BaseBungeeApi;
import org.lime.core.common.utils.Disposable;
import org.lime.core.fabric.utils.adapters.ResourceLocationIdentifierProxy;

@BindService
public class BungeeApi
        extends BaseBungeeApi<ServerPlayer> {
    private static final ResourceLocationIdentifierProxy CHANNEL_IDENTIFIER = ResourceLocationIdentifierProxy.parse(CHANNEL_ID);

    //#if PROPERTIES.versionMinecraft != '1.20.1'
    //IF//    private record Payload(byte[] data) implements CustomPacketPayload {
    //IF//        private static final Type<Payload> TYPE = new Type<>(CHANNEL_IDENTIFIER.handle());
    //IF//        private static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of((buffer, payload) -> buffer.writeBytes(payload.data()), buffer -> {
    //IF//            byte[] data = new byte[buffer.readableBytes()];
    //IF//            buffer.readBytes(data);
    //IF//            return new Payload(data);
    //IF//        });
    //IF//
    //IF//        static {
    //IF//            PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
    //IF//            PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    //IF//        }
    //IF//
    //IF//        @Override
    //IF//        public Type<Payload> type() {
    //IF//            return TYPE;
    //IF//        }
    //IF//    }
    //#endif

    @Inject MinecraftServer server;

    @Override
    public Disposable register() {
        //#if PROPERTIES.versionMinecraft != '1.20.1'
        //IF//        boolean registered = ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) ->
        //IF//                onPluginMessageReceived(context.player(), ByteStreams.newDataInput(payload.data())));
        //#else
        boolean registered = ServerPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER.handle(), (server, player, handler, buffer, responseSender) -> {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            server.execute(() -> onPluginMessageReceived(player, ByteStreams.newDataInput(data)));
        });
        //#endif
        if (!registered)
            throw new IllegalStateException("Bungee Messaging channel is already registered: " + CHANNEL_ID);
        return super.register();
    }

    @Override
    public void unregister() {
        ServerPlayNetworking.unregisterGlobalReceiver(CHANNEL_IDENTIFIER.handle());
        super.unregister();
    }

    @Override
    protected void sendBungeeMessage(ServerPlayer player, ByteArrayDataOutput output) {
        //#if PROPERTIES.versionMinecraft != '1.20.1'
        //IF//        ServerPlayNetworking.send(player, new Payload(output.toByteArray()));
        //#else
        ServerPlayNetworking.send(player, CHANNEL_IDENTIFIER.handle(), new FriendlyByteBuf(Unpooled.wrappedBuffer(output.toByteArray())));
        //#endif
    }

    @Override
    protected ServerPlayer getFirstPlayer() {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty())
            throw new IllegalArgumentException("Bungee Messaging Api requires at least one player to be online.");
        return players.get(0);
    }
}
