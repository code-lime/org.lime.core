package org.lime.core.fabric.services.buffers;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BasePacketEntityBufferStorage;
import org.lime.core.common.services.buffers.InjectBuffer;
import org.lime.core.common.services.buffers.PacketEntityViewSource;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.fabric.services.NativeComponent;
import org.lime.core.fabric.utils.WorldLocation;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

@BindService
public class PacketEntityBufferStorage
        extends BasePacketEntityBufferStorage<Entity, WorldLocation> {
    @Inject MinecraftServer server;
    @Inject NativeComponent nativeComponent;
    @Inject ServerLevel overworld;
    @Inject ScheduleTaskService taskService;
    @Inject Logger logger;

    private final Map<Class<? extends Entity>, EntityType<?>> entityTypes = new ConcurrentHashMap<>();
    private final Map<Entity, PacketTracker> trackers = new ConcurrentHashMap<>();
    private final Map<Entity, PacketEntityViewSource<
            ServerPlayer,
            EntityDataAccessor<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor>> pendingViewSources = new ConcurrentHashMap<>();
    private final TrackingPlayers trackingPlayers = new TrackingPlayers();
    private ScheduleTask tickTask = ScheduleTask.disabled();

    @Override
    public Disposable register() {
        initializeEntityTypes();
        tickTask = taskService.runLoop(this::tick, true, 1);
        return tickTask;
    }

    private synchronized void initializeEntityTypes() {
        if (!entityTypes.isEmpty())
            return;
        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
            var entity = create(entry.getValue(), overworld);
            if (entity != null)
                entityTypes.put(entity.getClass(), entry.getValue());
        });
    }

    @Override
    public void unregister() {
        tickTask.cancel();
        super.unregister();
        trackers.keySet().forEach(this::remove);
        pendingViewSources.clear();
        trackingPlayers.close();
    }

    @Override
    public <T extends Entity> PacketIterationEntityBuffer<T> entity(
            BaseEntityBufferSetup<WorldLocation> setup,
            Class<T> tClass) {
        return new PacketIterationEntityBuffer<>(this, setup, tClass);
    }

    @Override
    public <Index, T extends Entity> PacketIndexedEntityBuffer<Index, T> entity(
            BaseEntityBufferSetup<WorldLocation> setup,
            Class<Index> indexClass,
            Class<T> tClass) {
        return entity(setup, TypeLiteral.get(indexClass), tClass);
    }

    @Override
    public <Index, T extends Entity> PacketIndexedEntityBuffer<Index, T> entity(
            BaseEntityBufferSetup<WorldLocation> setup,
            TypeLiteral<Index> indexClass,
            Class<T> tClass) {
        return new PacketIndexedEntityBuffer<>(this, setup, indexClass, tClass);
    }

    @Override
    public BaseEntityBufferSetup<WorldLocation> createSetup(InjectBuffer injectBuffer) {
        return createSetup(
                injectBuffer.tag(),
                injectBuffer.entityKey(),
                injectBuffer.trackingDistance());
    }

    private EntityBufferSetup createSetup(String tag, String entityKey, int trackingDistance) {
        return new EntityBufferSetup(
                tag,
                Optional.of(entityKey)
                        .filter(value -> !value.isEmpty())
                        .map(Key::key),
                Optional.empty(),
                trackingDistance < 0 ? OptionalInt.empty() : OptionalInt.of(trackingDistance));
    }

    public PacketIterationEntityBuffer<Display.TextDisplay> text(EntityBufferSetup setup) {
        return entity(setup, Display.TextDisplay.class);
    }

    public PacketIterationEntityBuffer<Display.ItemDisplay> item(EntityBufferSetup setup) {
        return entity(setup, Display.ItemDisplay.class);
    }

    public PacketIterationEntityBuffer<Display.BlockDisplay> block(EntityBufferSetup setup) {
        return entity(setup, Display.BlockDisplay.class);
    }

    public PacketIterationEntityBuffer<Interaction> interact(EntityBufferSetup setup) {
        return entity(setup, Interaction.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, Display.TextDisplay> text(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, Display.TextDisplay.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, Display.ItemDisplay> item(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, Display.ItemDisplay.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, Display.BlockDisplay> block(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, Display.BlockDisplay.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, Interaction> interact(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, Interaction.class);
    }

    void attachView(
            Entity entity,
            PacketEntityViewSource<
                    ServerPlayer,
                    EntityDataAccessor<?>,
                    SynchedEntityData.DataValue<?>,
                    PacketEntityDataEditor> source) {
        Objects.requireNonNull(source, "source");
        runOnServerThread(() -> attachViewOnServerThread(entity, source));
    }

    private void attachViewOnServerThread(
            Entity entity,
            PacketEntityViewSource<
                    ServerPlayer,
                    EntityDataAccessor<?>,
                    SynchedEntityData.DataValue<?>,
                    PacketEntityDataEditor> source) {
        var tracker = trackers.get(entity);
        if (tracker != null) {
            tracker.attachView(source);
            return;
        }
        if (!entity.isRemoved())
            pendingViewSources.put(entity, source);
    }

    void refreshViews(
            Iterable<? extends Entity> entities,
            @Nullable EntityDataAccessor<?> trigger,
            @Nullable ServerPlayer player) {
        var snapshot = new ArrayList<Entity>();
        entities.forEach(snapshot::add);
        runOnServerThread(() -> snapshot.forEach(entity -> {
            var tracker = trackers.get(entity);
            if (tracker == null || trigger != null && !tracker.isTriggered(trigger))
                return;
            tracker.refreshView(player);
        }));
    }

    void refreshTracking(Iterable<? extends Entity> entities) {
        var snapshot = new ArrayList<Entity>();
        entities.forEach(snapshot::add);
        runOnServerThread(() -> {
            var players = new TrackingPlayers();
            snapshot.forEach(entity -> {
                var tracker = trackers.get(entity);
                if (tracker != null)
                    tracker.updateViewers(players);
            });
        });
    }

    private void runOnServerThread(Runnable action) {
        if (server.isSameThread())
            action.run();
        else
            server.execute(action);
    }

    void requireServerThread() {
        if (!server.isSameThread())
            throw new IllegalStateException("Packet entity view API must be used on the server thread");
    }

    @Override
    protected WorldLocation defaultLocation() {
        return new WorldLocation(overworld.dimension(), Vec3.ZERO, Vec2.ZERO);
    }

    @Override
    protected <T extends Entity> T spawn(
            WorldLocation location,
            Class<T> entityClass,
            @Nullable Key entityKey,
            Action1<T> setup) {
        var level = requireLevel(location);
        var entityType = getEntityType(entityClass, entityKey);
        var entity = create(entityType, level);
        if (entity == null)
            throw new IllegalArgumentException("Entity type " + entityType + " cannot be created");
        if (!entityClass.isInstance(entity)) {
            entity.discard();
            throw new IllegalArgumentException(
                    "Entity type " + entityType + " creates " + entity.getClass() + ", not " + entityClass);
        }

        var result = entityClass.cast(entity);
        move(result, location);
        try {
            setup.invoke(result);
            var trackingDistance = getTrackingRange(result)
                    .orElseGet(() -> entityType.clientTrackingRange() * 16);
            var tracker = new PacketTracker(
                    result,
                    level,
                    Math.max(0, trackingDistance),
                    pendingViewSources.remove(result));
            var previous = trackers.putIfAbsent(result, tracker);
            if (previous != null)
                throw new IllegalStateException("Duplicate packet entity id " + result.getId());
            return result;
        } catch (RuntimeException | Error exception) {
            pendingViewSources.remove(result);
            result.discard();
            throw exception;
        }
    }

    @Override
    protected void remove(Entity entity) {
        pendingViewSources.remove(entity);
        var tracker = trackers.remove(entity);
        if (tracker != null)
            tracker.close();
        entity.discard();
    }

    @Override
    protected void forEntities(Action1<Entity> consumer) {
        trackers.keySet().forEach(consumer);
    }

    @Override
    protected Set<String> getTags(Entity entity) {
        return entity.getTags();
    }

    @Override
    protected int getEntityId(Entity entity) {
        return entity.getId();
    }

    @Override
    protected boolean isValid(Entity entity) {
        return entity.isAlive() && trackers.containsKey(entity);
    }

    @Override
    protected WorldLocation getLocation(Entity entity) {
        return WorldLocation.of(entity);
    }

    @Override
    protected void teleport(Entity entity, WorldLocation location) {
        var tracker = trackers.get(entity);
        if (tracker == null)
            return;

        var level = requireLevel(location);
        if (tracker.level != level) {
            tracker.changeLevel(level, location);
        } else {
            move(entity, location);
        }
    }

    @Override
    protected boolean isEquals(
            @Nullable WorldLocation a,
            @Nullable WorldLocation b,
            boolean worldOnly) {
        return a == null
                ? b == null
                : b != null && (worldOnly
                        ? Objects.equals(a.levelKey(), b.levelKey())
                        : a.equals(b));
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> EntityType<T> getEntityType(
            Class<T> entityClass,
            @Nullable Key entityKey) {
        EntityType<?> result;
        if (entityKey == null) {
            result = entityTypes.get(entityClass);
            if (result == null) {
                initializeEntityTypes();
                result = entityTypes.get(entityClass);
            }
            if (result == null)
                throw new IllegalArgumentException("Entity class " + entityClass + " not supported");
        } else {
            result = BuiltInRegistries.ENTITY_TYPE.getOptional(nativeComponent.convert(entityKey))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Entity " + entityKey.asString() + " not found"));
        }
        return (EntityType<T>) result;
    }

    private ServerLevel requireLevel(WorldLocation location) {
        return Objects.requireNonNull(
                location.level(server),
                () -> "Level " + location.levelKey().location() + " is not loaded");
    }

    private static <T extends Entity> @Nullable T create(EntityType<T> entityType, ServerLevel level) {
        return entityType.create(level
                //#switch PROPERTIES.versionMinecraft
                //#caseofregex 1\.21\.[4-8]
                //OF//                , EntitySpawnReason.COMMAND
                //#default
                //#endswitch
        );
    }

    private static void move(Entity entity, WorldLocation location) {
        var position = location.position();
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.21.8
        //OF//        entity.snapTo(position.x, position.y, position.z, location.yaw(), location.pitch());
        //#default
        entity.moveTo(position.x, position.y, position.z, location.yaw(), location.pitch());
        //#endswitch
    }

    private void tick() {
        trackingPlayers.reset();
        trackers.values().forEach(tracker -> tracker.tick(trackingPlayers));
    }

    private static final class TrackingPlayers {
        private final Reference2ObjectOpenHashMap<
                ServerLevel,
                Long2ObjectOpenHashMap<ReferenceOpenHashSet<ServerPlayer>>> values =
                new Reference2ObjectOpenHashMap<>(4);
        private final ArrayDeque<Long2ObjectOpenHashMap<ReferenceOpenHashSet<ServerPlayer>>> reusable =
                new ArrayDeque<>();

        private void reset() {
            values.values().forEach(chunks -> {
                chunks.clear();
                reusable.addLast(chunks);
            });
            values.clear();
        }

        private void close() {
            values.clear();
            reusable.clear();
        }

        private ReferenceOpenHashSet<ServerPlayer> get(
                ServerLevel level,
                long chunk,
                Supplier<Collection<ServerPlayer>> factory) {
            Long2ObjectOpenHashMap<ReferenceOpenHashSet<ServerPlayer>> chunks = values.get(level);
            if (chunks == null) {
                chunks = reusable.pollFirst();
                if (chunks == null)
                    chunks = new Long2ObjectOpenHashMap<>();
                values.put(level, chunks);
            }

            ReferenceOpenHashSet<ServerPlayer> players = chunks.get(chunk);
            if (players == null) {
                Collection<ServerPlayer> source = factory.get();
                players = new ReferenceOpenHashSet<>(source.size());
                players.addAll(source);
                chunks.put(chunk, players);
            }
            return players;
        }
    }

    private final class PacketTracker {
        private final Entity entity;
        private final int trackingDistance;
        private final Set<ServerPlayer> viewers = new ReferenceOpenHashSet<>(4);
        private final Set<ServerPlayer> desiredViewers = new ReferenceOpenHashSet<>(4);
        private final Map<UUID, Map<Integer, PacketEntityDataEditor.Entry<?>>> viewOverlays = new HashMap<>();
        private final @Nullable EnumMap<EquipmentSlot, ItemStack> equipmentSnapshot;
        private ServerLevel level;
        private ServerEntity serverEntity;
        private @Nullable PacketEntityViewSource<
                ServerPlayer,
                EntityDataAccessor<?>,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor> viewSource;

        private PacketTracker(
                Entity entity,
                ServerLevel level,
                int trackingDistance,
                @Nullable PacketEntityViewSource<
                        ServerPlayer,
                        EntityDataAccessor<?>,
                        SynchedEntityData.DataValue<?>,
                        PacketEntityDataEditor> viewSource) {
            this.entity = entity;
            this.trackingDistance = trackingDistance;
            this.level = level;
            this.viewSource = viewSource;
            this.equipmentSnapshot = createEquipmentSnapshot();
            this.serverEntity = createServerEntity();
        }

        private void attachView(PacketEntityViewSource<
                ServerPlayer,
                EntityDataAccessor<?>,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor> viewSource) {
            this.viewSource = viewSource;
            updateViewers(new TrackingPlayers());
            refreshView(null);
        }

        private boolean isTriggered(EntityDataAccessor<?> trigger) {
            return viewSource != null && viewSource.isTriggeredProperty(trigger);
        }

        private PacketEntityVisibility visibility() {
            return viewSource == null
                    ? PacketEntityVisibility.all()
                    : Objects.requireNonNull(viewSource.visibility(), "packet entity visibility");
        }

        private @Nullable EnumMap<EquipmentSlot, ItemStack> createEquipmentSnapshot() {
            if (!(entity instanceof LivingEntity livingEntity))
                return null;

            var snapshot = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
            for (var slot : EquipmentSlot.values())
                snapshot.put(slot, livingEntity.getItemBySlot(slot).copy());
            return snapshot;
        }

        private ServerEntity createServerEntity() {
            var type = entity.getType();
            return new ServerEntity(
                    level,
                    entity,
                    type.updateInterval(),
                    type.trackDeltas(),
                    this::broadcast
                    //#switch PROPERTIES.versionMinecraft
                    //#caseof 1.21.8
                    //OF//                    , this::broadcast
                    //#default
                    //#endswitch
            );
        }

        private void broadcast(Packet<?> packet) {
            broadcast(packet, player -> true);
        }

        private void broadcast(Packet<?> packet, List<UUID> ignoredPlayers) {
            broadcast(packet, player -> !ignoredPlayers.contains(player.getUUID()));
        }

        private void broadcast(Packet<?> packet, Predicate<ServerPlayer> sendTo) {
            var refresh = packet instanceof ClientboundSetEntityDataPacket metadata
                    && metadata.id() == entity.getId()
                    && hasDirtyViewTrigger(metadata.packedItems());
            viewers.forEach(player -> {
                if (!sendTo.test(player))
                    return;
                var previous = viewOverlays.getOrDefault(player.getUUID(), Map.of());
                var current = refresh ? recomputeView(player, false) : previous;
                player.connection.send(applyView(packet, previous, current));
            });
        }

        private boolean hasDirtyViewTrigger(List<SynchedEntityData.DataValue<?>> values) {
            var source = viewSource;
            return source != null
                    && source.hasListeners()
                    && values.stream().anyMatch(source::isTriggeredUpdate);
        }

        private Packet<?> applyView(
                Packet<?> packet,
                Map<Integer, PacketEntityDataEditor.Entry<?>> previous,
                Map<Integer, PacketEntityDataEditor.Entry<?>> current) {
            if (!(packet instanceof ClientboundSetEntityDataPacket metadata)
                    || metadata.id() != entity.getId()) {
                return packet;
            }

            if (previous.isEmpty() && current.isEmpty())
                return packet;

            if (previous.equals(current)) {
                boolean hasOverriddenValue = false;
                for (var value : metadata.packedItems()) {
                    if (current.containsKey(value.id())) {
                        hasOverriddenValue = true;
                        break;
                    }
                }
                if (!hasOverriddenValue)
                    return packet;
            }

            var values = new Int2ObjectLinkedOpenHashMap<SynchedEntityData.DataValue<?>>(
                    metadata.packedItems().size() + previous.size() + current.size());
            metadata.packedItems().forEach(value -> values.put(value.id(), value));
            previous.forEach((id, value) -> {
                if (!current.containsKey(id))
                    values.put(id.intValue(), canonicalValue(value));
            });
            current.forEach((id, value) -> values.put(id.intValue(), value.value()));
            return new ClientboundSetEntityDataPacket(metadata.id(), List.copyOf(values.values()));
        }

        private void refreshView(@Nullable ServerPlayer player) {
            if (player == null) {
                viewers.forEach(viewer -> recomputeView(viewer, true));
            } else if (viewers.contains(player)) {
                recomputeView(player, true);
            }
        }

        private Map<Integer, PacketEntityDataEditor.Entry<?>> recomputeView(
                ServerPlayer player,
                boolean sendDelta) {
            var playerId = player.getUUID();
            var previous = viewOverlays.getOrDefault(playerId, Map.of());
            var source = viewSource;
            if (source == null || !source.hasListeners()) {
                if (previous.isEmpty())
                    return previous;
                viewOverlays.remove(playerId);
                if (sendDelta)
                    sendViewDelta(player, previous, Map.of());
                return Map.of();
            }

            var editor = new PacketEntityDataEditor(entity.getEntityData());
            try {
                source.edit(player, editor);
            } catch (Throwable exception) {
                logger.error(
                        "Unable to build packet entity metadata view for entity {} and player {}",
                        entity.getId(),
                        playerId,
                        exception);
                return previous;
            }

            var current = editor.snapshot();
            if (previous.equals(current))
                return previous;

            if (current.isEmpty())
                viewOverlays.remove(playerId);
            else
                viewOverlays.put(playerId, current);

            if (sendDelta)
                sendViewDelta(player, previous, current);
            return current;
        }

        private void sendViewDelta(
                ServerPlayer player,
                Map<Integer, PacketEntityDataEditor.Entry<?>> previous,
                Map<Integer, PacketEntityDataEditor.Entry<?>> current) {
            var delta = new Int2ObjectLinkedOpenHashMap<SynchedEntityData.DataValue<?>>(
                    previous.size() + current.size());
            previous.forEach((id, oldValue) -> {
                var newValue = current.get(id);
                if (newValue == null)
                    delta.put(id.intValue(), canonicalValue(oldValue));
                else if (!oldValue.value().equals(newValue.value()))
                    delta.put(id.intValue(), newValue.value());
            });
            current.forEach((id, newValue) -> {
                if (!previous.containsKey(id))
                    delta.put(id.intValue(), newValue.value());
            });

            if (!delta.isEmpty()) {
                player.connection.send(new ClientboundSetEntityDataPacket(
                        entity.getId(),
                        List.copyOf(delta.values())));
            }
        }

        private <Value> SynchedEntityData.DataValue<Value> canonicalValue(
                PacketEntityDataEditor.Entry<Value> entry) {
            var property = entry.property();
            return SynchedEntityData.DataValue.create(property, entity.getEntityData().get(property));
        }

        private void addPairing(ServerPlayer player) {
            var overlay = recomputeView(player, false);
            //#switch PROPERTIES.versionMinecraft
            //#caseof 1.20.1
            var packets = new ArrayList<Packet<ClientGamePacketListener>>();
            //#default
            //OF// var packets = new ArrayList<Packet<? super ClientGamePacketListener>>();
            //#endswitch
            serverEntity.sendPairingData(player, packets::add);
            if (!overlay.isEmpty()) {
                var metadata = new ArrayList<SynchedEntityData.DataValue<?>>();
                overlay.values().forEach(value -> metadata.add(value.value()));
                packets.add(new ClientboundSetEntityDataPacket(
                        entity.getId(),
                        metadata));
            }
            player.connection.send(new ClientboundBundlePacket(packets));
            entity.startSeenByPlayer(player);
        }

        private void tick(TrackingPlayers trackingPlayers) {
            if (entity.isRemoved()) {
                trackers.remove(entity, this);
                close();
                return;
            }

            var loadedLevel = server.getLevel(level.dimension());
            if (loadedLevel == null) {
                clearViewers();
                return;
            }
            if (loadedLevel != level) {
                clearViewers();
                level = loadedLevel;
                entity.setLevel(loadedLevel);
                serverEntity = createServerEntity();
            }

            updateViewers(trackingPlayers);
            sendEquipmentChanges();
            serverEntity.sendChanges();
        }

        private void updateViewers(TrackingPlayers trackingPlayers) {
            desiredViewers.clear();
            var visibility = visibility();
            if (!visibility.defaultVisible() && visibility.exceptions().isEmpty()) {
                reconcileViewers();
                return;
            }

            var playerRange = server.getScaledTrackingDistance(trackingDistance);
            if (playerRange <= 0) {
                reconcileViewers();
                return;
            }

            var chunkPos = new ChunkPos(entity.blockPosition());
            var trackedPlayers = trackingPlayers.get(
                    level,
                    chunkPos.toLong(),
                    () -> PlayerLookup.tracking(level, chunkPos));
            var maxDistanceSquared = (double) playerRange * playerRange;

            if (visibility.defaultVisible()) {
                for (var player : trackedPlayers) {
                    if (visibility.isVisible(player.getUUID())
                            && isTrackingCandidate(player, maxDistanceSquared))
                        desiredViewers.add(player);
                }
            } else {
                for (var playerId : visibility.exceptions()) {
                    var player = server.getPlayerList().getPlayer(playerId);
                    if (player != null
                            && trackedPlayers.contains(player)
                            && isTrackingCandidate(player, maxDistanceSquared))
                        desiredViewers.add(player);
                }
            }

            reconcileViewers();
        }

        private boolean isTrackingCandidate(ServerPlayer player, double maxDistanceSquared) {
            var dx = entity.getX() - player.getX();
            var dz = entity.getZ() - player.getZ();
            return player.level() == level
                    && !player.isRemoved()
                    && dx * dx + dz * dz <= maxDistanceSquared
                    && entity.broadcastToPlayer(player);
        }

        private void reconcileViewers() {
            viewers.removeIf(player -> {
                if (desiredViewers.remove(player))
                    return false;
                serverEntity.removePairing(player);
                viewOverlays.remove(player.getUUID());
                return true;
            });
            desiredViewers.forEach(player -> {
                addPairing(player);
                viewers.add(player);
            });
            desiredViewers.clear();
        }

        private void sendEquipmentChanges() {
            if (equipmentSnapshot == null || !(entity instanceof LivingEntity livingEntity))
                return;

            var changes = new ArrayList<Pair<EquipmentSlot, ItemStack>>();
            for (var slot : EquipmentSlot.values()) {
                var current = livingEntity.getItemBySlot(slot);
                if (ItemStack.matches(equipmentSnapshot.get(slot), current))
                    continue;

                var snapshot = current.copy();
                equipmentSnapshot.put(slot, snapshot);
                changes.add(Pair.of(slot, snapshot));
            }

            if (!changes.isEmpty())
                broadcast(new ClientboundSetEquipmentPacket(entity.getId(), changes));
        }

        private void changeLevel(ServerLevel newLevel, WorldLocation location) {
            clearViewers();
            level = newLevel;
            entity.setLevel(newLevel);
            move(entity, location);
            serverEntity = createServerEntity();
        }

        private void clearViewers() {
            viewers.forEach(serverEntity::removePairing);
            viewers.clear();
            desiredViewers.clear();
            viewOverlays.clear();
        }

        private void close() {
            clearViewers();
            viewOverlays.clear();
        }
    }
}
