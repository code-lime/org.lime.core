package org.lime.core.paper.services.buffers;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BasePacketEntityBufferStorage;
import org.lime.core.common.services.buffers.InjectBuffer;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.execute.Action1;
import org.spigotmc.TrackingRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Owns detached NMS entities and mirrors their state to nearby players through
 * a standalone {@link ServerEntity}. The entities are never added to a world.
 */
@BindService
public class PacketEntityBufferStorage
        extends BasePacketEntityBufferStorage<Entity, Location> {
    private final ConcurrentHashMap<Integer, PacketEntityHandle> entities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Entity>, Optional<EntityType>> apiEntityTypes = new ConcurrentHashMap<>();

    @Inject World defaultWorld;
    @Inject ScheduleTaskService taskService;

    private ScheduleTask tickTask = ScheduleTask.disabled();

    @Override
    public Disposable register() {
        tickTask = taskService.runLoop(this::tick, true, 1);
        return tickTask;
    }

    @Override
    public void unregister() {
        tickTask.cancel();
        super.unregister();
        entities.values().forEach(PacketEntityHandle::destroy);
        entities.clear();
    }

    private void tick() {
        entities.values().forEach(PacketEntityHandle::tick);
    }

    private EntityType getApiEntityType(Class<? extends Entity> entityClass) {
        return apiEntityTypes.computeIfAbsent(entityClass, value -> {
            for (var type : EntityType.values()) {
                var apiClass = type.getEntityClass();
                if (apiClass != null && apiClass.isAssignableFrom(entityClass))
                    return Optional.of(type);
            }
            return Optional.empty();
        }).orElseThrow(() -> new IllegalArgumentException("Entity class " + entityClass + " not supported"));
    }

    private static EntityBufferSetup setup(
            String tag,
            String entityKey,
            int trackingDistance) {
        return new EntityBufferSetup(
                tag,
                Optional.of(entityKey)
                        .filter(value -> !value.isEmpty())
                        .map(Key::key),
                Optional.empty(),
                trackingDistance < 0 ? OptionalInt.empty() : OptionalInt.of(trackingDistance));
    }

    @Override
    public BaseEntityBufferSetup<Location> createSetup(InjectBuffer injectBuffer) {
        return setup(injectBuffer.tag(), injectBuffer.entityKey(), injectBuffer.trackingDistance());
    }

    @Override
    public <T extends Entity> PacketIterationEntityBuffer<T> entity(
            BaseEntityBufferSetup<Location> setup,
            Class<T> tClass) {
        return new PacketIterationEntityBuffer<>(this, setup, tClass);
    }

    @Override
    public <Index, T extends Entity> PacketIndexedEntityBuffer<Index, T> entity(
            BaseEntityBufferSetup<Location> setup,
            Class<Index> indexClass,
            Class<T> tClass) {
        return new PacketIndexedEntityBuffer<>(this, setup, TypeLiteral.get(indexClass), tClass);
    }

    @Override
    public <Index, T extends Entity> PacketIndexedEntityBuffer<Index, T> entity(
            BaseEntityBufferSetup<Location> setup,
            TypeLiteral<Index> indexClass,
            Class<T> tClass) {
        return new PacketIndexedEntityBuffer<>(this, setup, indexClass, tClass);
    }

    public PacketIterationEntityBuffer<TextDisplay> text(EntityBufferSetup setup) {
        return entity(setup, TextDisplay.class);
    }

    public PacketIterationEntityBuffer<ItemDisplay> item(EntityBufferSetup setup) {
        return entity(setup, ItemDisplay.class);
    }

    public PacketIterationEntityBuffer<BlockDisplay> block(EntityBufferSetup setup) {
        return entity(setup, BlockDisplay.class);
    }

    public PacketIterationEntityBuffer<Interaction> interact(EntityBufferSetup setup) {
        return entity(setup, Interaction.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, TextDisplay> text(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, TextDisplay.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, ItemDisplay> item(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, ItemDisplay.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, BlockDisplay> block(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, BlockDisplay.class);
    }

    public <Index> PacketIndexedEntityBuffer<Index, Interaction> interact(
            EntityBufferSetup setup,
            Class<Index> indexClass) {
        return entity(setup, indexClass, Interaction.class);
    }

    @Override
    protected Location defaultLocation() {
        return new Location(defaultWorld, 0, 0, 0);
    }

    @Override
    protected <T extends Entity> T spawn(
            Location location,
            Class<T> entityClass,
            @Nullable Key entityKey,
            Action1<T> setup) {
        World world = Objects.requireNonNull(location.getWorld(), "Packet entity location has no world");
        EntityType bukkitType;
        if (entityKey == null) {
            bukkitType = getApiEntityType(entityClass);
        } else {
            bukkitType = Registry.ENTITY_TYPE.get(entityKey);
            if (bukkitType == null)
                throw new IllegalArgumentException("Entity " + entityKey.asString() + " not found");
        }

        ServerLevel level = ((CraftWorld)world).getHandle();
        net.minecraft.world.entity.EntityType<?> nmsType = CraftEntityType.bukkitToMinecraft(bukkitType);
        net.minecraft.world.entity.Entity nmsEntity = nmsType.create(level, EntitySpawnReason.COMMAND);
        if (nmsEntity == null)
            throw new IllegalArgumentException("Entity " + bukkitType.getKey() + " cannot be created");

        nmsEntity.snapTo(
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());

        T apiEntity;
        try {
            apiEntity = entityClass.cast(nmsEntity.getBukkitEntity());
            setup.invoke(apiEntity);
        } catch (RuntimeException | Error exception) {
            nmsEntity.discard();
            throw exception;
        }

        int trackingRange = getTrackingRange(apiEntity).orElseGet(() -> TrackingRange.getEntityTrackingRange(
                nmsEntity,
                nmsType.clientTrackingRange() * 16));
        PacketEntityHandle handle = new PacketEntityHandle(apiEntity, nmsEntity, trackingRange);
        PacketEntityHandle previous = entities.putIfAbsent(nmsEntity.getId(), handle);
        if (previous != null) {
            nmsEntity.discard();
            throw new IllegalStateException("Duplicate packet entity id " + nmsEntity.getId());
        }
        return apiEntity;
    }

    @Override
    protected void remove(Entity entity) {
        PacketEntityHandle handle = getHandle(entity);
        if (handle != null && entities.remove(entity.getEntityId(), handle))
            handle.destroy();
    }

    @Override
    protected void forEntities(Action1<Entity> entityLoad) {
        entities.values().forEach(handle -> entityLoad.invoke(handle.apiEntity));
    }

    @Override
    protected Set<String> getTags(Entity entity) {
        return entity.getScoreboardTags();
    }

    @Override
    protected int getEntityId(Entity entity) {
        return entity.getEntityId();
    }

    @Override
    protected boolean isValid(Entity entity) {
        PacketEntityHandle handle = getHandle(entity);
        return handle != null && !handle.removed && !handle.nmsEntity.isRemoved();
    }

    @Override
    protected Location getLocation(Entity entity) {
        PacketEntityHandle handle = Objects.requireNonNull(getHandle(entity), "Unknown packet entity " + entity.getEntityId());
        return handle.location();
    }

    @Override
    protected void teleport(Entity entity, Location location) {
        PacketEntityHandle handle = Objects.requireNonNull(getHandle(entity), "Unknown packet entity " + entity.getEntityId());
        handle.move(location);
    }

    @Override
    protected boolean isEquals(@Nullable Location a, @Nullable Location b, boolean worldOnly) {
        return a == null
                ? b == null
                : b != null
                  && (worldOnly
                      ? Objects.equals(a.getWorld(), b.getWorld())
                      : a.equals(b));
    }

    private @Nullable PacketEntityHandle getHandle(Entity entity) {
        PacketEntityHandle handle = entities.get(entity.getEntityId());
        return handle != null && handle.apiEntity == entity ? handle : null;
    }

    private final class PacketEntityHandle
            implements ServerEntity.Synchronizer {
        private final Entity apiEntity;
        private final net.minecraft.world.entity.Entity nmsEntity;
        private final int trackingRange;
        private final Set<ServerPlayerConnection> viewers = Collections.newSetFromMap(new IdentityHashMap<>());
        private final @Nullable EnumMap<EquipmentSlot, ItemStack> equipmentSnapshot;

        private ServerEntity serverEntity;
        private boolean removed;

        private PacketEntityHandle(
                Entity apiEntity,
                net.minecraft.world.entity.Entity nmsEntity,
                int trackingRange) {
            this.apiEntity = apiEntity;
            this.nmsEntity = nmsEntity;
            this.trackingRange = trackingRange;
            this.equipmentSnapshot = createEquipmentSnapshot();
            this.serverEntity = createServerEntity();
        }

        private @Nullable EnumMap<EquipmentSlot, ItemStack> createEquipmentSnapshot() {
            if (!(nmsEntity instanceof LivingEntity livingEntity))
                return null;

            EnumMap<EquipmentSlot, ItemStack> snapshot = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : EquipmentSlot.VALUES)
                snapshot.put(slot, livingEntity.getItemBySlot(slot).copy());
            return snapshot;
        }

        private ServerLevel level() {
            return (ServerLevel)nmsEntity.level();
        }

        private ServerEntity createServerEntity() {
            var type = nmsEntity.getType();
            return new ServerEntity(
                    level(),
                    nmsEntity,
                    type.updateInterval(),
                    type.trackDeltas(),
                    this,
                    viewers);
        }

        private Location location() {
            return new Location(
                    level().getWorld(),
                    nmsEntity.getX(),
                    nmsEntity.getY(),
                    nmsEntity.getZ(),
                    nmsEntity.getYRot(),
                    nmsEntity.getXRot());
        }

        private void move(Location location) {
            if (removed)
                return;

            World world = Objects.requireNonNull(location.getWorld(), "Packet entity location has no world");
            ServerLevel newLevel = ((CraftWorld)world).getHandle();
            if (newLevel != level()) {
                clearViewers();
                nmsEntity.setLevel(newLevel);
                nmsEntity.snapTo(
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch());
                serverEntity = createServerEntity();
                return;
            }

            nmsEntity.snapTo(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch());
        }

        private int trackingRange() {
            return level().getServer().getScaledTrackingDistance(Math.max(0, trackingRange));
        }

        private void tick() {
            if (removed)
                return;
            if (nmsEntity.isRemoved()) {
                entities.remove(nmsEntity.getId(), this);
                destroy();
                return;
            }

            ServerLevel currentLevel = level();
            ServerLevel activeLevel = currentLevel.getServer().getLevel(currentLevel.dimension());
            if (activeLevel == null) {
                clearViewers();
                return;
            }
            if (activeLevel != currentLevel) {
                clearViewers();
                nmsEntity.setLevel(activeLevel);
                serverEntity = createServerEntity();
            }

            int range = trackingRange();
            var chunk = nmsEntity.chunkPosition();
            World world = level().getWorld();
            Set<ServerPlayerConnection> desired = Collections.newSetFromMap(new IdentityHashMap<>());

            if (range > 0) {
                for (Player player : world.getPlayersSeeingChunk(chunk.x, chunk.z)) {
                    if (!player.isOnline() || player.getWorld() != world || !player.canSee(apiEntity))
                        continue;

                    int playerRange = Math.min(range, player.getSendViewDistance() * 16);
                    double dx = player.getX() - nmsEntity.getX();
                    double dz = player.getZ() - nmsEntity.getZ();
                    if (dx * dx + dz * dz > (double)playerRange * playerRange)
                        continue;

                    ServerPlayer serverPlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle();
                    if (!nmsEntity.broadcastToPlayer(serverPlayer))
                        continue;
                    desired.add(serverPlayer.connection);
                }
            }

            for (ServerPlayerConnection connection : new ArrayList<>(viewers)) {
                if (desired.remove(connection))
                    continue;
                if (viewers.remove(connection))
                    serverEntity.removePairing(connection.getPlayer());
            }

            for (ServerPlayerConnection connection : desired) {
                if (!viewers.add(connection))
                    continue;
                ServerPlayer player = connection.getPlayer();
                serverEntity.addPairing(player);
                serverEntity.onPlayerAdd();
            }

            sendEquipmentChanges();
            serverEntity.sendChanges();
        }

        private void sendEquipmentChanges() {
            if (equipmentSnapshot == null || !(nmsEntity instanceof LivingEntity livingEntity))
                return;

            var changes = new ArrayList<Pair<EquipmentSlot, ItemStack>>();
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack current = livingEntity.getItemBySlot(slot);
                if (ItemStack.matches(equipmentSnapshot.get(slot), current))
                    continue;

                ItemStack snapshot = current.copy();
                equipmentSnapshot.put(slot, snapshot);
                changes.add(Pair.of(slot, snapshot));
            }

            if (!changes.isEmpty())
                sendToTrackingPlayers(new ClientboundSetEquipmentPacket(nmsEntity.getId(), changes));
        }

        private void clearViewers() {
            for (ServerPlayerConnection connection : new ArrayList<>(viewers)) {
                if (viewers.remove(connection))
                    serverEntity.removePairing(connection.getPlayer());
            }
        }

        private void destroy() {
            if (removed)
                return;
            removed = true;
            clearViewers();
            nmsEntity.discard();
        }

        @Override
        public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
            viewers.forEach(connection -> connection.send(packet));
        }

        @Override
        public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
            sendToTrackingPlayers(packet);
        }

        @Override
        public void sendToTrackingPlayersFiltered(
                Packet<? super ClientGamePacketListener> packet,
                Predicate<ServerPlayer> filter) {
            viewers.forEach(connection -> {
                if (filter.test(connection.getPlayer()))
                    connection.send(packet);
            });
        }
    }
}
