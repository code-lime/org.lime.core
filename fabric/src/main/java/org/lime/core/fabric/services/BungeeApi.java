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

    //#switch PROPERTIES.versionMinecraft
    //#caseof 1.20.1
    //#default
    //OF//    private record Payload(byte[] data) implements CustomPacketPayload {
    //OF//        private static final Type<Payload> TYPE = new Type<>(CHANNEL_IDENTIFIER.handle());
    //OF//        private static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of((buffer, payload) -> buffer.writeBytes(payload.data()), buffer -> {
    //OF//            byte[] data = new byte[buffer.readableBytes()];
    //OF//            buffer.readBytes(data);
    //OF//            return new Payload(data);
    //OF//        });
    //OF//
    //OF//        static {
    //OF//            registerPayload();
    //OF//        }
    //OF//
    //OF//        @Override
    //OF//        public Type<Payload> type() {
    //OF//            return TYPE;
    //OF//        }
    //OF//    }
    //#endswitch

    private static void registerPayload() {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        //#caseofregex ^1\.21\.[0-9]+$
        //OF//        PayloadTypeRegistry.playC2S().register(Payload.TYPE, Payload.CODEC);
        //OF//        PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
        //#default
        //OF//        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        //OF//        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
        //#endswitch
    }

    @Inject MinecraftServer server;

    @Override
    public Disposable register() {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        boolean registered = ServerPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER.handle(), (server, player, handler, buffer, responseSender) -> {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            server.execute(() -> onPluginMessageReceived(player, ByteStreams.newDataInput(data)));
        });
        //#default
        //OF//        boolean registered = ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) ->
        //OF//                onPluginMessageReceived(context.player(), ByteStreams.newDataInput(payload.data())));
        //#endswitch
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
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        ServerPlayNetworking.send(player, CHANNEL_IDENTIFIER.handle(), new FriendlyByteBuf(Unpooled.wrappedBuffer(output.toByteArray())));
        //#default
        //OF//        ServerPlayNetworking.send(player, new Payload(output.toByteArray()));
        //#endswitch
    }

    @Override
    protected ServerPlayer getFirstPlayer() {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty())
            throw new IllegalArgumentException("Bungee Messaging Api requires at least one player to be online.");
        return players.get(0);
    }
}
