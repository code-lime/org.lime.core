package org.lime.core.fabric.services.buffers;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
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
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.fabric.services.NativeComponent;
import org.lime.core.fabric.utils.WorldLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@BindService
public class PacketEntityBufferStorage
        extends BasePacketEntityBufferStorage<Entity, WorldLocation> {
    @Inject MinecraftServer server;
    @Inject NativeComponent nativeComponent;
    @Inject ServerLevel overworld;
    @Inject ScheduleTaskService taskService;

    private final Map<Class<? extends Entity>, EntityType<?>> entityTypes = new ConcurrentHashMap<>();
    private final Map<Entity, PacketTracker> trackers = new ConcurrentHashMap<>();
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
            trackers.put(result, new PacketTracker(result, level, Math.max(0, trackingDistance)));
            return result;
        } catch (RuntimeException | Error exception) {
            result.discard();
            throw exception;
        }
    }

    @Override
    protected void remove(Entity entity) {
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
        trackers.values().forEach(PacketTracker::tick);
    }

    private final class PacketTracker {
        private final Entity entity;
        private final int trackingDistance;
        private final Set<ServerPlayer> viewers = Collections.newSetFromMap(new IdentityHashMap<>());
        private final @Nullable EnumMap<EquipmentSlot, ItemStack> equipmentSnapshot;
        private ServerLevel level;
        private ServerEntity serverEntity;

        private PacketTracker(Entity entity, ServerLevel level, int trackingDistance) {
            this.entity = entity;
            this.trackingDistance = trackingDistance;
            this.level = level;
            this.equipmentSnapshot = createEquipmentSnapshot();
            this.serverEntity = createServerEntity();
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
            viewers.forEach(player -> player.connection.send(packet));
        }

        private void broadcast(Packet<?> packet, List<UUID> ignoredPlayers) {
            viewers.forEach(player -> {
                if (!ignoredPlayers.contains(player.getUUID()))
                    player.connection.send(packet);
            });
        }

        private void tick() {
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

            var candidates = Collections.newSetFromMap(new IdentityHashMap<ServerPlayer, Boolean>());
            var chunkPos = new ChunkPos(entity.blockPosition());
            var playerRange = server.getScaledTrackingDistance(trackingDistance);
            var maxDistanceSquared = (double) playerRange * playerRange;
            for (var player : PlayerLookup.tracking(level, chunkPos)) {
                var dx = entity.getX() - player.getX();
                var dz = entity.getZ() - player.getZ();
                if (player.level() == level
                        && !player.isRemoved()
                        && entity.broadcastToPlayer(player)
                        && dx * dx + dz * dz <= maxDistanceSquared) {
                    candidates.add(player);
                }
            }

            viewers.removeIf(player -> {
                if (candidates.remove(player))
                    return false;
                serverEntity.removePairing(player);
                return true;
            });
            candidates.forEach(player -> {
                serverEntity.addPairing(player);
                viewers.add(player);
            });
            sendEquipmentChanges();
            serverEntity.sendChanges();
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
        }

        private void close() {
            clearViewers();
        }
    }
}
