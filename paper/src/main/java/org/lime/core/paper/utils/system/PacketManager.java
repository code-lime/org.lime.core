package org.lime.core.paper.utils.system;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import net.minecraft.data.info.PacketReport;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerPlayerConnection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.Unsafe;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.system.ListBuilder;
import org.lime.core.common.utils.system.execute.Action1;
import org.lime.core.common.utils.system.execute.Action2;
import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

            private record PacketIndex(String protocol, String flow, String type) {
                public static PacketIndex of(PacketType.Protocol protocol, net.minecraft.network.protocol.PacketType<?> type) {
                    return new PacketIndex(protocol.getMojangName(), type.flow().id(), type.id().toString());
                }
            }

            private static final Map<PacketIndex, Integer> packetIndexes;
            static {
                JsonElement result = (JsonElement)ReflectionMethod.ofMojang(PacketReport.class, "serializePackets")
                    .call(new PacketReport(null), new Object[0]);
                packetIndexes = new ConcurrentHashMap<>();
                result.getAsJsonObject()
                        .asMap()
                        .forEach((protocol, unbound) -> unbound
                                .getAsJsonObject()
                                .asMap()
                                .forEach((flow, packets) -> packets
                                        .getAsJsonObject()
                                        .asMap()
                                        .forEach((type, data) -> packetIndexes.put(
                                                new PacketIndex(protocol, flow, type),
                                                data.getAsJsonObject()
                                                        .get("protocol_id")
                                                        .getAsInt()))));
            }
            private static Optional<Tuple2<Integer, PacketType.Protocol>> findPacketData(
                    net.minecraft.network.protocol.PacketType<?> type) {
                for (PacketType.Protocol protocol : PacketType.Protocol.values()) {
                    var index = packetIndexes.get(PacketIndex.of(protocol, type));
                    if (index != null)
                        return Optional.of(Tuple.of(index, protocol));
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
            public Disposable listen() {
                if (receiving.isEmpty() && sending.isEmpty())
                    return Disposable.empty();

                Adapter listener = build();
                ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
                protocolManager.addPacketListener(listener);
                return () -> protocolManager.removePacketListener(listener);
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
