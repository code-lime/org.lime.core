package org.lime.core.paper.system;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Streams;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.network.ServerPlayerConnection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.lime.core.common.Unsafe;
import org.lime.core.common.system.ListBuilder;
import org.lime.core.common.system.execute.*;
import org.lime.core.common.system.tuple.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PacketManager {
    public static boolean send(Player player, Packet<?> packet) {
        if (!(player instanceof CraftPlayer handle)) return false;
        ServerPlayerConnection playerConnection = handle.getHandle().connection;
        if (playerConnection == null) return false;
        playerConnection.send(packet);
        return true;
    }
    public static boolean send(Player player, List<Packet<?>> packets) {
        if (!(player instanceof CraftPlayer handle)) return false;
        ServerPlayerConnection playerConnection = handle.getHandle().connection;
        if (playerConnection == null) return false;
        packets.forEach(playerConnection::send);
        return true;
    }
    public static boolean send(Player player, Stream<Packet<?>> packets) {
        if (!(player instanceof CraftPlayer handle)) return false;
        ServerPlayerConnection playerConnection = handle.getHandle().connection;
        if (playerConnection == null) return false;
        packets.forEach(playerConnection::send);
        return true;
    }
    public static boolean send(Player player, Packet<?>... packets) {
        return send(player, Arrays.asList(packets));
    }

    public static class Adapter extends PacketAdapter {
        public static class Builder {
            private static final ConcurrentMap<Class<?>, ConcurrentLinkedQueue<PacketType>> classToPacketTypes = Streams.stream(PacketType.values())
                    .flatMap(type -> Optional.ofNullable(type.getPacketClass())
                            .map(v -> Tuple.of(v, type))
                            .stream())
                    .collect(Collectors.groupingByConcurrent(v -> v.val0, Collectors.mapping(v -> v.val1, Collectors.toCollection(ConcurrentLinkedQueue::new))));

            public final Plugin plugin;
            private Builder(Plugin plugin) {
                this.plugin = plugin;
            }

            private final HashMap<PacketType, List<Action2<PacketType, PacketEvent>>> receiving = new HashMap<>();
            private final HashMap<PacketType, List<Action2<PacketType, PacketEvent>>> sending = new HashMap<>();

            public Builder add(PacketType type, Action2<PacketType, PacketEvent> func) {
                return add(Collections.singletonList(type), func);
            }
            public Builder add(PacketType type, Action1<PacketEvent> func) {
                return add(Collections.singletonList(type), func);
            }

            private static Optional<Integer> findPacketIndex(
                    net.minecraft.network.protocol.PacketType<?> type,
                    ProtocolInfo.Unbound<?, ?> template) {
                List<Integer> indexList = new ArrayList<>();

                template.listPackets((packetType, index) -> {
                    if (packetType.equals(type))
                        indexList.add(index);
                });

                return indexList.isEmpty() ? Optional.empty() : Optional.of(indexList.getFirst());
            }
            private static Optional<Tuple2<Integer, PacketType.Protocol>> findPacketData(
                    net.minecraft.network.protocol.PacketType<?> type) {
                Map<PacketType.Protocol, ProtocolInfo.Unbound<?, ?>> templates = new HashMap<>();
                switch (type.flow()) {
                    case CLIENTBOUND -> {
                        templates.put(PacketType.Protocol.STATUS, StatusProtocols.CLIENTBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.LOGIN, LoginProtocols.CLIENTBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.CONFIGURATION, ConfigurationProtocols.CLIENTBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.PLAY, GameProtocols.CLIENTBOUND_TEMPLATE);
                    }
                    case SERVERBOUND -> {
                        templates.put(PacketType.Protocol.HANDSHAKING, HandshakeProtocols.SERVERBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.STATUS, StatusProtocols.SERVERBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.LOGIN, LoginProtocols.SERVERBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.CONFIGURATION, ConfigurationProtocols.SERVERBOUND_TEMPLATE);
                        templates.put(PacketType.Protocol.PLAY, GameProtocols.SERVERBOUND_TEMPLATE);
                    }
                }
                for (var kv : templates.entrySet()) {
                    var index = findPacketIndex(type, kv.getValue());
                    if (index.isPresent())
                        return Optional.of(Tuple.of(index.get(), kv.getKey()));
                }
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            public <T extends Packet<?>>Builder add(Class<T> packetClass, Action2<T, PacketEvent> func) {
                var types = classToPacketTypes.get(packetClass);
                if (types == null || types.isEmpty()) {
                    var rawType = Unsafe.createInstance(packetClass).type();

                    var sender = switch (rawType.flow()) {
                        case CLIENTBOUND -> PacketType.Sender.SERVER;
                        case SERVERBOUND -> PacketType.Sender.CLIENT;
                    };
                    Tuple2<Integer, PacketType.Protocol> dat = findPacketData(rawType)
                            .orElseThrow(() -> new IllegalArgumentException("Packet class '"+packetClass+"' not has packet type"));

                    int index = dat.val0;
                    PacketType.Protocol protocol = dat.val1;

                    PacketType type = PacketType.fromCurrent(protocol, sender, index, packetClass);
                    if (types == null)
                        types = classToPacketTypes.compute(packetClass, (_,v) -> v == null ? new ConcurrentLinkedQueue<>() : v);
                    types.add(type);
                }
                Builder builder = this;
                Action1<PacketEvent> nextFunc = e -> func.invoke((T)e.getPacket().getHandle(), e);
                for (PacketType type : types)
                    builder = builder.add(type, nextFunc);
                return builder;
            }

            public Builder add(PacketType[] types, Action2<PacketType, PacketEvent> func) {
                return add(Arrays.asList(types), func);
            }
            public Builder add(PacketType[] types, Action1<PacketEvent> func) {
                return add(Arrays.asList(types), func);
            }
            public Builder add(List<PacketType> types, Action2<PacketType, PacketEvent> func) {
                types.forEach(type -> (type.isServer() ? sending : receiving).compute(type, (k, v) -> {
                    if (v == null) v = new ArrayList<>();
                    v.add(func);
                    return v;
                }));
                return this;
            }
            public Builder add(List<PacketType> types, Action1<PacketEvent> func) {
                return add(types, (_,b) -> func.invoke(b));
            }

            public Adapter build() {
                return new Adapter(this);
            }
            public void listen() {
                if (!receiving.isEmpty() || !sending.isEmpty())
                    ProtocolLibrary.getProtocolManager().addPacketListener(build());
            }
        }
        private final HashMap<PacketType, List<Action2<PacketType, PacketEvent>>> receiving = new HashMap<>();
        private final HashMap<PacketType, List<Action2<PacketType, PacketEvent>>> sending = new HashMap<>();
        private Adapter(Builder builder) {
            super(builder.plugin, ListBuilder.<PacketType>of().add(builder.receiving.keySet()).add(builder.sending.keySet()).build());
            this.receiving.putAll(builder.receiving);
            this.sending.putAll(builder.sending);
        }
        private void invoke(PacketEvent event, HashMap<PacketType, List<Action2<PacketType, PacketEvent>>> map) {
            PacketType type = event.getPacketType();
            List<Action2<PacketType, PacketEvent>> list = map.getOrDefault(type, null);
            if (list == null) return;
            list.forEach(func -> func.invoke(type, event));
        }
        @Override public void onPacketReceiving(PacketEvent event) {
            invoke(event, receiving);
        }
        @Override public void onPacketSending(PacketEvent event) {
            invoke(event, sending);
        }
    }

    public static Adapter.Builder adapter(Plugin plugin) {
        return new Adapter.Builder(plugin);
    }
}
