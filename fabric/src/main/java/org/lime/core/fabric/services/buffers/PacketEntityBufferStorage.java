package org.lime.core.fabric.services.buffers;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BasePacketEntityBufferStorage;
import org.lime.core.common.services.buffers.InjectBuffer;
import org.lime.core.common.services.buffers.PacketEntityBatch;
import org.lime.core.common.services.buffers.PacketEntityBufferState;
import org.lime.core.common.services.buffers.PacketEntityMetadataState;
import org.lime.core.common.services.buffers.PacketEntityStorage;
import org.lime.core.common.services.buffers.PacketEntityTracker;
import org.lime.core.common.services.buffers.PacketEntityTrackingCache;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.fabric.hooks.PacketEntityInteractionHook;
import org.lime.core.fabric.hooks.PacketEntitySendHook;
import org.lime.core.fabric.services.NativeComponent;
import org.lime.core.fabric.utils.WorldLocation;

import java.util.ArrayList;
import java.util.EnumMap;
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
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

    private record PendingEntityContext(@NotNull PacketEntityBufferState.EntitySource<ServerPlayer, SynchedEntityData.DataValue<?>, PacketEntityDataEditor, Packet<?>> source, @NotNull PacketEntityBatch<ServerPlayer, Packet<?>> batch) {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void sendBundle(@NotNull ServerPlayer player, @NotNull List<Packet<?>> packets) {
        player.connection.send(new ClientboundBundlePacket((Iterable)packets));
    }

    @Inject MinecraftServer server;
    @Inject NativeComponent nativeComponent;
    @Inject ServerLevel overworld;

    private final Map<Class<? extends Entity>, EntityType<?>> entityTypes = new ConcurrentHashMap<>();
    private final BufferBackend bufferBackend = new BufferBackend();
    private final PacketEntityStorage<
            Entity,
            PendingEntityContext,
            PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer>,
            PacketTracker> entityStorage = new PacketEntityStorage<>(
                    bufferBackend::checkAccess,
                    Entity::getId,
                    this::createTracker,
                    Entity::discard,
                    PacketEntityBufferStorage::createTrackingCache);
    private Disposable interactionRegistration = Disposable.empty();

    @Override
    public @NotNull Disposable register() {
        initializeEntityTypes();
        interactionRegistration = PacketEntityInteractionHook.register(this::interact);
        return interactionRegistration;
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
        Disposable registration = interactionRegistration;
        interactionRegistration = Disposable.empty();
        var storage = entityStorage;
        try (storage) {
            try {
                registration.close();
            } finally {
                super.unregister();
            }
        }
    }

    @Override
    public <T extends Entity> @NotNull PacketIterationEntityBuffer<T> entity(@NotNull BaseEntityBufferSetup<WorldLocation> setup, @NotNull Class<T> tClass) {
        return new PacketIterationEntityBuffer<>(this, setup, tClass);
    }

    @Override
    public <Index, T extends Entity> @NotNull PacketIndexedEntityBuffer<Index, T> entity(@NotNull BaseEntityBufferSetup<WorldLocation> setup, @NotNull Class<Index> indexClass, @NotNull Class<T> tClass) {
        return entity(setup, TypeLiteral.get(indexClass), tClass);
    }

    @Override
    public <Index, T extends Entity> @NotNull PacketIndexedEntityBuffer<Index, T> entity(@NotNull BaseEntityBufferSetup<WorldLocation> setup, @NotNull TypeLiteral<Index> indexClass, @NotNull Class<T> tClass) {
        return new PacketIndexedEntityBuffer<>(this, setup, indexClass, tClass);
    }

    @Override
    public @NotNull BaseEntityBufferSetup<WorldLocation> createSetup(@NotNull InjectBuffer injectBuffer) {
        return createSetup(injectBuffer.tag(), injectBuffer.entityKey(), injectBuffer.trackingDistance());
    }

    private @NotNull EntityBufferSetup createSetup(@NotNull String tag, @NotNull String entityKey, int trackingDistance) {
        return new EntityBufferSetup(tag, Optional.of(entityKey).filter(value -> !value.isEmpty()).map(Key::key), Optional.empty(), trackingDistance < 0 ? OptionalInt.empty() : OptionalInt.of(trackingDistance));
    }

    public @NotNull PacketIterationEntityBuffer<Display.TextDisplay> text(@NotNull EntityBufferSetup setup) {
        return entity(setup, Display.TextDisplay.class);
    }

    public @NotNull PacketIterationEntityBuffer<Display.ItemDisplay> item(@NotNull EntityBufferSetup setup) {
        return entity(setup, Display.ItemDisplay.class);
    }

    public @NotNull PacketIterationEntityBuffer<Display.BlockDisplay> block(@NotNull EntityBufferSetup setup) {
        return entity(setup, Display.BlockDisplay.class);
    }

    public @NotNull PacketIterationEntityBuffer<Interaction> interact(@NotNull EntityBufferSetup setup) {
        return entity(setup, Interaction.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, Display.TextDisplay> text(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, Display.TextDisplay.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, Display.ItemDisplay> item(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, Display.ItemDisplay.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, Display.BlockDisplay> block(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, Display.BlockDisplay.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, Interaction> interact(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, Interaction.class);
    }

    <Index, Type extends Entity> @NotNull PacketEntityBufferState<
            Index,
            Type,
            ServerPlayer,
            PacketEntityDataEditor.PropertyAccess<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor,
            Packet<?>> packetState(@NotNull Map<Index, Type> bufferEntities, @NotNull Map<Index, PacketEntityVisibility> visibility) {
        return new PacketEntityBufferState<>(bufferBackend, bufferEntities, visibility, PacketEntityDataEditor.PropertyAccess::matches);
    }

    private boolean interact(@NotNull ServerPlayer player, int entityId, @NotNull ServerboundInteractPacket packet) {
        PacketTracker tracker = entityStorage.tracker(entityId);
        return tracker != null && tracker.interact(player, packet);
    }

    private final class BufferBackend implements PacketEntityBufferState.Backend<
            Entity,
            ServerPlayer,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor,
            Packet<?>> {
        @Override
        public void checkAccess() {
            if (!server.isSameThread())
                throw new IllegalStateException("Packet entity view API must be used on the server thread");
        }

        @Override
        public @NotNull PacketEntityBatch<ServerPlayer, Packet<?>> createBatch() {
            return new PacketEntityBatch<>(BundlerInfo.BUNDLE_SIZE_LIMIT, PacketEntityBufferStorage::sendBundle, packet -> packet instanceof ClientboundBundlePacket bundle ? bundle.subPackets() : null);
        }

        @Override
        public void attach(@NotNull Entity entity, @NotNull PacketEntityBufferState.EntitySource<ServerPlayer, SynchedEntityData.DataValue<?>, PacketEntityDataEditor, Packet<?>> source, @NotNull PacketEntityBatch<ServerPlayer, Packet<?>> batch) {
            entityStorage.attach(entity, new PendingEntityContext(source, batch));
        }

        @Override
        public void synchronize(@NotNull Iterable<? extends Entity> bufferEntities) {
            entityStorage.synchronize(bufferEntities);
        }
    }

    @Override
    protected @NotNull WorldLocation defaultLocation() {
        return new WorldLocation(overworld.dimension(), Vec3.ZERO, Vec2.ZERO);
    }

    @Override
    protected <T extends Entity> @NotNull T spawn(@NotNull WorldLocation location, @NotNull Class<T> entityClass, @Nullable Key entityKey, @NotNull Action1<T> setup) {
        var level = requireLevel(location);
        var entityType = getEntityType(entityClass, entityKey);
        var entity = create(entityType, level);
        if (entity == null)
            throw new IllegalArgumentException("Entity type " + entityType + " cannot be created");
        if (!entityClass.isInstance(entity)) {
            entity.discard();
            throw new IllegalArgumentException("Entity type " + entityType + " creates " + entity.getClass() + ", not " + entityClass);
        }

        var result = entityClass.cast(entity);
        return entityStorage.registerSpawn(result, () -> {
            move(result, location);
            setup.invoke(result);
        });
    }

    @Override
    protected void remove(@NotNull Entity entity) {
        entityStorage.remove(entity);
    }

    @Override
    protected @NotNull Set<String> getTags(@NotNull Entity entity) {
        return entity.getTags();
    }

    @Override
    protected int getEntityId(@NotNull Entity entity) {
        return entity.getId();
    }

    @Override
    protected boolean isValid(@NotNull Entity entity) {
        return entity.isAlive() && entityStorage.tracker(entity) != null;
    }

    @Override
    protected @NotNull WorldLocation getLocation(@NotNull Entity entity) {
        return WorldLocation.of(entity);
    }

    @Override
    protected void teleport(@NotNull Entity entity, @NotNull WorldLocation location) {
        var tracker = entityStorage.tracker(entity);
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
    protected boolean isEquals(@Nullable WorldLocation a, @Nullable WorldLocation b, boolean worldOnly) {
        return a == null
                ? b == null
                : b != null && (worldOnly
                        ? Objects.equals(a.levelKey(), b.levelKey())
                        : a.equals(b));
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> @NotNull EntityType<T> getEntityType(@NotNull Class<T> entityClass, @Nullable Key entityKey) {
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
                    .orElseThrow(() -> new IllegalArgumentException("Entity " + entityKey.asString() + " not found"));
        }
        return (EntityType<T>) result;
    }

    private @NotNull ServerLevel requireLevel(@NotNull WorldLocation location) {
        ServerLevel level = location.level(server);
        if (level == null) {
            throw new IllegalArgumentException("Level " + location.levelKey().location() + " is not loaded");
        }
        return level;
    }

    private static <T extends Entity> @Nullable T create(@NotNull EntityType<T> entityType, @NotNull ServerLevel level) {
        return entityType.create(level
                //#switch PROPERTIES.versionMinecraft
                //#caseofregex 1\.21\.[4-8]
                //OF//                , EntitySpawnReason.COMMAND
                //#default
                //#endswitch
        );
    }

    private static void move(@NotNull Entity entity, @NotNull WorldLocation location) {
        var position = location.position();
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.21.8
        //OF//        entity.snapTo(position.x, position.y, position.z, location.yaw(), location.pitch());
        //#default
        entity.moveTo(position.x, position.y, position.z, location.yaw(), location.pitch());
        //#endswitch
    }

    private static @NotNull PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer> createTrackingCache() {
        return new PacketEntityTrackingCache<>(Object2ObjectOpenHashMap::new);
    }

    private @NotNull PacketTracker createTracker(@NotNull Entity entity, @NotNull PendingEntityContext context) {
        int trackingDistance = getTrackingRange(entity)
                .orElseGet(() -> entity.getType().clientTrackingRange() * 16);
        return new PacketTracker(entity, (ServerLevel)entity.level(), Math.max(0, trackingDistance), context);
    }

    private final class PacketTracker
            implements PacketEntityStorage.Tracker<
                    PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer>>,
                    PacketEntityTracker.Driver<ServerPlayer, Packet<?>> {
        private final Entity entity;
        private final int trackingDistance;
        private final PacketEntityMetadataState<
                ServerPlayer,
                SynchedEntityData,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor,
                Packet<?>> metadataState;
        private final @Nullable EnumMap<EquipmentSlot, ItemStack> equipmentSnapshot;
        private final PacketEntityBufferState.EntitySource<ServerPlayer, SynchedEntityData.DataValue<?>, PacketEntityDataEditor, Packet<?>> source;
        private final PacketEntityBatch<ServerPlayer, Packet<?>> packetBatch;
        private final PacketEntityTracker<ServerPlayer, Packet<?>> tracker;
        private ServerLevel level;
        private ServerEntity serverEntity;
        private boolean pendingRebind;

        private PacketTracker(@NotNull Entity entity, @NotNull ServerLevel level, int trackingDistance, @NotNull PendingEntityContext context) {
            this.entity = entity;
            this.trackingDistance = trackingDistance;
            this.level = level;
            this.source = context.source();
            this.metadataState = new PacketEntityMetadataState<>(source, createMetadataCodec());
            this.equipmentSnapshot = createEquipmentSnapshot();
            this.packetBatch = context.batch();
            this.tracker = new PacketEntityTracker<>(this, packetBatch, source);
            this.serverEntity = createServerEntity();
        }

        @Override
        public void synchronize(@NotNull PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer> cache) {
            if (!prepareLevel())
                return;

            int range = server.getScaledTrackingDistance(trackingDistance);
            ChunkPos chunk = entity.chunkPosition();
            List<ServerPlayer> players = range > 0
                    ? cache.get(level, chunk, () -> level.getChunkSource().chunkMap.getPlayers(chunk, false))
                    : List.of();
            tracker.synchronize(players, range);
        }

        @Override
        public void close() {
            tracker.close();
        }

        private boolean prepareLevel() {
            if (entity.isRemoved()) {
                entityStorage.detach(entity, this);
                tracker.close();
                return false;
            }

            ServerLevel currentLevel = level;
            ServerLevel loadedLevel = server.getLevel(currentLevel.dimension());
            if (loadedLevel == null) {
                tracker.clearAudience();
                return false;
            }
            if (loadedLevel != currentLevel || pendingRebind) {
                tracker.clearAudience();
                if (loadedLevel != currentLevel) {
                    level = loadedLevel;
                    entity.setLevel(loadedLevel);
                    serverEntity = createServerEntity();
                }
                pendingRebind = false;
            }
            return true;
        }

        private boolean interact(@NotNull ServerPlayer player, @NotNull ServerboundInteractPacket packet) {
            if (!source.hasInteractionListeners())
                return false;
            source.interact(player, PacketEntityInteractionHook.decode(packet));
            return true;
        }

        private @Nullable EnumMap<EquipmentSlot, ItemStack> createEquipmentSnapshot() {
            if (!(entity instanceof LivingEntity livingEntity))
                return null;
            EnumMap<EquipmentSlot, ItemStack> result = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : EQUIPMENT_SLOTS)
                result.put(slot, livingEntity.getItemBySlot(slot).copy());
            return result;
        }

        private @NotNull PacketEntityMetadataState.Codec<
                SynchedEntityData,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor,
                Packet<?>> createMetadataCodec() {
            return new PacketEntityMetadataState.Codec<>(
                    () -> new PacketEntityDataEditor(entity.getEntityData()),
                    SynchedEntityData.DataValue::id,
                    entries -> new ClientboundSetEntityDataPacket(entity.getId(), entries),
                    packet -> packet instanceof ClientboundSetEntityDataPacket metadata
                            && metadata.id() == entity.getId()
                            ? metadata.packedItems()
                            : null);
        }

        private @NotNull ServerEntity createServerEntity() {
            var type = entity.getType();
            ServerEntity result = new ServerEntity(
                    level,
                    entity,
                    1,
                    type.trackDeltas(),
                    this::broadcast
                    //#switch PROPERTIES.versionMinecraft
                    //#caseof 1.21.8
                    //OF//                    , this::broadcast
                    //#default
                    //#endswitch
            );
            PacketEntitySendHook.mark(result);
            return result;
        }

        private void broadcast(@NotNull Packet<?> packet) {
            tracker.broadcast(packet);
        }

        private void broadcast(@NotNull Packet<?> packet, @NotNull List<UUID> ignoredPlayers) {
            tracker.broadcast(packet, player -> !ignoredPlayers.contains(player.getUUID()));
        }

        private void changeLevel(@NotNull ServerLevel newLevel, @NotNull WorldLocation location) {
            level = newLevel;
            entity.setLevel(newLevel);
            move(entity, location);
            serverEntity = createServerEntity();
            pendingRebind = true;
        }

        @Override
        public boolean canTrack(@NotNull ServerPlayer player, int trackingRange) {
            if (!source.isVisible(player.getUUID()) || player.level() != level || player.isRemoved())
                return false;
            var dx = entity.getX() - player.getX();
            var dz = entity.getZ() - player.getZ();
            var maxDistanceSquared = (double)trackingRange * trackingRange;
            return dx * dx + dz * dz <= maxDistanceSquared
                    && entity.broadcastToPlayer(player);
        }

        @Override
        public void addPairing(@NotNull ServerPlayer player) {
            ArrayList<Packet<?>> packets = new ArrayList<>();
            serverEntity.sendPairingData(player, packets::add);
            packets = packetBatch.flatten(packets);
            metadataState.pairing(player, packets);
            packetBatch.sendLeaves(player, packets);
        }

        @Override
        public void removePairing(@NotNull ServerPlayer player) {
            packetBatch.send(player, new ClientboundRemoveEntitiesPacket(entity.getId()));
            metadataState.forget(player);
        }

        @Override
        public @NotNull Packet<?> personalize(@NotNull ServerPlayer player, @NotNull Packet<?> packet) {
            return metadataState.personalize(player, packet);
        }

        @Override
        public void collectStatePackets() {
            if (equipmentSnapshot != null) {
                LivingEntity livingEntity = (LivingEntity)entity;
                ArrayList<Pair<EquipmentSlot, ItemStack>> changes = null;
                for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                    ItemStack current = livingEntity.getItemBySlot(slot);
                    if (ItemStack.matches(equipmentSnapshot.get(slot), current))
                        continue;
                    ItemStack copy = current.copy();
                    equipmentSnapshot.put(slot, copy);
                    if (changes == null)
                        changes = new ArrayList<>();
                    changes.add(Pair.of(slot, copy));
                }
                if (changes != null)
                    tracker.broadcast(new ClientboundSetEquipmentPacket(entity.getId(), changes));
            }
            serverEntity.sendChanges();
        }

        @Override
        public void onClose() {
            entity.discard();
        }
    }
}
