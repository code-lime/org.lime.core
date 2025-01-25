package org.lime.system;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Streams;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerPlayerConnection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Action2;
import org.lime.system.tuple.Tuple;

import java.util.*;
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
            private static final Map<Class<?>, List<PacketType>> classToPacketTypes = Streams.stream(PacketType.values())
                    .flatMap(type -> Optional.of(type.getPacketClass())
                            .map(v -> Tuple.of(v, type))
                            .stream())
                    .collect(Collectors.groupingBy(v -> v.val0, Collectors.mapping(v -> v.val1, Collectors.toList())));

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

            @SuppressWarnings("unchecked")
            public <T extends Packet<?>>Builder add(Class<T> packetClass, Action2<T, PacketEvent> func) {
                List<PacketType> types = classToPacketTypes.get(packetClass);
                if (types == null) return this;
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
                return add(types,  (a,b) -> func.invoke(b));
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
