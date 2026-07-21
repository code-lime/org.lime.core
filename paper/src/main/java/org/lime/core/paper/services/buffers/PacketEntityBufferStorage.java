package org.lime.core.paper.services.buffers;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.BindService;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BasePacketEntityBufferStorage;
import org.lime.core.common.services.buffers.InjectBuffer;
import org.lime.core.common.services.buffers.PacketEntityBatch;
import org.lime.core.common.services.buffers.PacketEntityBufferState;
import org.lime.core.common.services.buffers.PacketEntityInteraction;
import org.lime.core.common.services.buffers.PacketEntityMetadataState;
import org.lime.core.common.services.buffers.PacketEntityStorage;
import org.lime.core.common.services.buffers.PacketEntityTracker;
import org.lime.core.common.services.buffers.PacketEntityTrackingCache;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action6;
import org.lime.core.common.utils.execute.Func6;
import org.spigotmc.TrackingRange;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Owns detached NMS entities and mirrors their state to nearby players through
 * a standalone {@link ServerEntity}. The entities are never added to a world.
 */
@BindService
public class PacketEntityBufferStorage
        extends BasePacketEntityBufferStorage<Entity, Location>
        implements Listener {
    private record PendingEntityContext(@NotNull PacketEntityBufferState.EntitySource<ServerPlayer, SynchedEntityData.DataValue<?>, PacketEntityDataEditor, Packet<? super ClientGamePacketListener>> source, @NotNull PacketEntityBatch<ServerPlayer, Packet<? super ClientGamePacketListener>> batch) {}

    private static final Optional<Class<?>> serverEntitySynchronizerClass =
            Reflection.findClassOptional("net.minecraft.server.level.ServerEntity$Synchronizer");
    private static final Action6<
            net.minecraft.world.entity.Entity,
            Double,
            Double,
            Double,
            Float,
            Float> moveEntity = ReflectionMethod.ofMojangOptional(
                    net.minecraft.world.entity.Entity.class,
                    "snapTo",
                    double.class,
                    double.class,
                    double.class,
                    float.class,
                    float.class)
            .orElseGet(() -> ReflectionMethod.ofMojang(
                    net.minecraft.world.entity.Entity.class,
                    "moveTo",
                    double.class,
                    double.class,
                    double.class,
                    float.class,
                    float.class))
            .lambda(Action6.class);
    private static final Func6<
            ServerLevel,
            net.minecraft.world.entity.Entity,
            Integer,
            Boolean,
            Object,
            Set<ServerPlayerConnection>,
            @NotNull ServerEntity> serverEntityConstructor = ReflectionConstructor.of(
                    ServerEntity.class,
                    ServerLevel.class,
                    net.minecraft.world.entity.Entity.class,
                    int.class,
                    boolean.class,
                    serverEntitySynchronizerClass.orElse(Consumer.class),
                    Set.class)
            .lambda(Func6.class);

    private static void moveEntity(@NotNull net.minecraft.world.entity.Entity entity, @NotNull Location location) {
        moveEntity.invoke(
                entity,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    private final BufferBackend bufferBackend = new BufferBackend();
    private final PacketEntityStorage<
            Entity,
            PendingEntityContext,
            PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer>,
            PacketEntityHandle> entities = new PacketEntityStorage<>(
                    bufferBackend::checkAccess,
                    Entity::getEntityId,
                    this::createTracker,
                    entity -> ((CraftEntity)entity).getHandle().discard(),
                    PacketEntityBufferStorage::createTrackingCache);
    private final Map<Class<? extends Entity>, Optional<EntityType>> apiEntityTypes =
            new Object2ObjectOpenHashMap<>();

    @Inject World defaultWorld;

    @Override
    public @NotNull Disposable register() {
        return Disposable.empty();
    }

    @Override
    public void unregister() {
        try (entities) {
            super.unregister();
        }
    }

    private static @NotNull PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer> createTrackingCache() {
        return new PacketEntityTrackingCache<>(Object2ObjectOpenHashMap::new);
    }

    private @NotNull EntityType getApiEntityType(@NotNull Class<? extends Entity> entityClass) {
        return apiEntityTypes.computeIfAbsent(entityClass, value -> {
            for (var type : EntityType.values()) {
                var apiClass = type.getEntityClass();
                if (apiClass != null && apiClass.isAssignableFrom(entityClass))
                    return Optional.of(type);
            }
            return Optional.empty();
        }).orElseThrow(() -> new IllegalArgumentException("Entity class " + entityClass + " not supported"));
    }

    private static @NotNull EntityBufferSetup setup(@NotNull String tag, @NotNull String entityKey, int trackingDistance) {
        return new EntityBufferSetup(tag, Optional.of(entityKey).filter(value -> !value.isEmpty()).map(Key::key), Optional.empty(), trackingDistance < 0 ? OptionalInt.empty() : OptionalInt.of(trackingDistance));
    }

    @Override
    public @NotNull BaseEntityBufferSetup<Location> createSetup(@NotNull InjectBuffer injectBuffer) {
        return setup(injectBuffer.tag(), injectBuffer.entityKey(), injectBuffer.trackingDistance());
    }

    @Override
    public <T extends Entity> @NotNull PacketIterationEntityBuffer<T> entity(@NotNull BaseEntityBufferSetup<Location> setup, @NotNull Class<T> tClass) {
        return new PacketIterationEntityBuffer<>(this, setup, tClass);
    }

    @Override
    public <Index, T extends Entity> @NotNull PacketIndexedEntityBuffer<Index, T> entity(@NotNull BaseEntityBufferSetup<Location> setup, @NotNull Class<Index> indexClass, @NotNull Class<T> tClass) {
        return new PacketIndexedEntityBuffer<>(this, setup, TypeLiteral.get(indexClass), tClass);
    }

    @Override
    public <Index, T extends Entity> @NotNull PacketIndexedEntityBuffer<Index, T> entity(@NotNull BaseEntityBufferSetup<Location> setup, @NotNull TypeLiteral<Index> indexClass, @NotNull Class<T> tClass) {
        return new PacketIndexedEntityBuffer<>(this, setup, indexClass, tClass);
    }

    public @NotNull PacketIterationEntityBuffer<TextDisplay> text(@NotNull EntityBufferSetup setup) {
        return entity(setup, TextDisplay.class);
    }

    public @NotNull PacketIterationEntityBuffer<ItemDisplay> item(@NotNull EntityBufferSetup setup) {
        return entity(setup, ItemDisplay.class);
    }

    public @NotNull PacketIterationEntityBuffer<BlockDisplay> block(@NotNull EntityBufferSetup setup) {
        return entity(setup, BlockDisplay.class);
    }

    public @NotNull PacketIterationEntityBuffer<Interaction> interact(@NotNull EntityBufferSetup setup) {
        return entity(setup, Interaction.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, TextDisplay> text(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, TextDisplay.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, ItemDisplay> item(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, ItemDisplay.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, BlockDisplay> block(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, BlockDisplay.class);
    }

    public <Index> @NotNull PacketIndexedEntityBuffer<Index, Interaction> interact(@NotNull EntityBufferSetup setup, @NotNull Class<Index> indexClass) {
        return entity(setup, indexClass, Interaction.class);
    }

    @Override
    protected @NotNull Location defaultLocation() {
        return new Location(defaultWorld, 0, 0, 0);
    }

    @Override
    protected <T extends Entity> @NotNull T spawn(@NotNull Location location, @NotNull Class<T> entityClass, @Nullable Key entityKey, @NotNull Action1<T> setup) {
        World world = location.getWorld();
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

        boolean completed = false;
        try {
            T apiEntity = entityClass.cast(nmsEntity.getBukkitEntity());
            T registered = entities.registerSpawn(apiEntity, () -> {
                moveEntity(nmsEntity, location);
                setup.invoke(apiEntity);
            });
            completed = true;
            return registered;
        } finally {
            if (!completed && !nmsEntity.isRemoved())
                nmsEntity.discard();
        }
    }

    @Override
    protected void remove(@NotNull Entity entity) {
        entities.remove(entity);
    }

    @Override
    protected @NotNull Set<String> getTags(@NotNull Entity entity) {
        return entity.getScoreboardTags();
    }

    @Override
    protected int getEntityId(@NotNull Entity entity) {
        return entity.getEntityId();
    }

    @Override
    protected boolean isValid(@NotNull Entity entity) {
        PacketEntityHandle handle = entities.tracker(entity);
        return handle != null && !handle.nmsEntity.isRemoved();
    }

    @Override
    protected @NotNull Location getLocation(@NotNull Entity entity) {
        return entities.tracker(entity).location();
    }

    @Override
    protected void teleport(@NotNull Entity entity, @NotNull Location location) {
        entities.tracker(entity).move(location);
    }

    @Override
    protected boolean isEquals(@Nullable Location a, @Nullable Location b, boolean worldOnly) {
        return a == null
                ? b == null
                : b != null && (worldOnly
                        ? Objects.equals(a.getWorld(), b.getWorld())
                        : a.equals(b));
    }

    <Index, Type extends Entity> @NotNull PacketEntityBufferState<
            Index,
            Type,
            ServerPlayer,
            PacketEntityDataEditor.PropertyAccess<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor,
            Packet<? super ClientGamePacketListener>> packetState(@NotNull Map<Index, Type> bufferEntities, @NotNull Map<Index, PacketEntityVisibility> visibility) {
        return new PacketEntityBufferState<>(bufferBackend, bufferEntities, visibility, PacketEntityDataEditor.PropertyAccess::matches);
    }

    @EventHandler
    public void onUseUnknownEntity(@NotNull PlayerUseUnknownEntityEvent event) {
        PacketEntityHandle handle = entities.tracker(event.getEntityId());
        if (handle != null)
            handle.interact(((CraftPlayer)event.getPlayer()).getHandle(), event);
    }

    private static @NotNull PacketEntityInteraction interaction(@NotNull PlayerUseUnknownEntityEvent event) {
        boolean secondary = event.getPlayer().isSneaking();
        if (event.isAttack())
            return PacketEntityInteraction.attack(secondary);

        PacketEntityInteraction.Hand hand =
                event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                        ? PacketEntityInteraction.Hand.OFF_HAND
                        : PacketEntityInteraction.Hand.MAIN_HAND;
        Vector clicked = event.getClickedRelativePosition();
        PacketEntityInteraction.Position position = clicked == null
                ? null
                : new PacketEntityInteraction.Position(clicked.getX(), clicked.getY(), clicked.getZ());
        return PacketEntityInteraction.interact(hand, position, secondary);
    }

    private final class BufferBackend implements PacketEntityBufferState.Backend<
            Entity,
            ServerPlayer,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor,
            Packet<? super ClientGamePacketListener>> {
        @Override
        public void checkAccess() {
            if (!Bukkit.isPrimaryThread())
                throw new IllegalStateException("Packet entity view API must be used on the server thread");
        }

        @Override
        public @NotNull PacketEntityBatch<ServerPlayer, Packet<? super ClientGamePacketListener>> createBatch() {
            return new PacketEntityBatch<>(BundlerInfo.BUNDLE_SIZE_LIMIT, (player, packets) -> player.connection.send(new ClientboundBundlePacket(packets)), packet -> packet instanceof ClientboundBundlePacket bundle ? bundle.subPackets() : null);
        }

        @Override
        public void attach(@NotNull Entity entity, @NotNull PacketEntityBufferState.EntitySource<ServerPlayer, SynchedEntityData.DataValue<?>, PacketEntityDataEditor, Packet<? super ClientGamePacketListener>> source, @NotNull PacketEntityBatch<ServerPlayer, Packet<? super ClientGamePacketListener>> batch) {
            entities.attach(entity, new PendingEntityContext(source, batch));
        }

        @Override
        public void synchronize(@NotNull Iterable<? extends Entity> bufferEntities) {
            entities.synchronize(bufferEntities);
        }
    }

    private @NotNull PacketEntityHandle createTracker(@NotNull Entity entity, @NotNull PendingEntityContext context) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity)entity).getHandle();
        var nmsType = nmsEntity.getType();
        int trackingRange = getTrackingRange(entity).orElseGet(() -> TrackingRange.getEntityTrackingRange(nmsEntity, nmsType.clientTrackingRange() * 16));
        return new PacketEntityHandle(entity, nmsEntity, trackingRange, context);
    }

    private final class PacketEntityHandle
            implements PacketEntityStorage.Tracker<
                    PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer>>,
            PacketEntityTracker.Driver<
                    ServerPlayer,
                    Packet<? super ClientGamePacketListener>> {
        private final Entity apiEntity;
        private final net.minecraft.world.entity.Entity nmsEntity;
        private final int trackingRange;
        private final PacketEntityBatch<ServerPlayer, Packet<? super ClientGamePacketListener>> batch;
        private final PacketEntityBufferState.EntitySource<ServerPlayer, SynchedEntityData.DataValue<?>, PacketEntityDataEditor, Packet<? super ClientGamePacketListener>> source;
        private final PacketEntityMetadataState<
                ServerPlayer,
                SynchedEntityData,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor,
                Packet<? super ClientGamePacketListener>> metadataState;
        private final @Nullable EnumMap<EquipmentSlot, ItemStack> equipment;
        private final PacketEntityTracker<
                ServerPlayer,
                Packet<? super ClientGamePacketListener>> tracker;
        private ServerEntity serverEntity;
        private boolean pendingRebind;

        private PacketEntityHandle(@NotNull Entity apiEntity, @NotNull net.minecraft.world.entity.Entity nmsEntity, int trackingRange, @NotNull PendingEntityContext context) {
            this.apiEntity = apiEntity;
            this.nmsEntity = nmsEntity;
            this.trackingRange = trackingRange;
            this.batch = context.batch();
            this.source = context.source();
            this.metadataState = new PacketEntityMetadataState<>(source, createMetadataCodec());
            this.equipment = createEquipmentSnapshot();
            this.tracker = new PacketEntityTracker<>(this, batch, source);
            this.serverEntity = createServerEntity();
        }

        private @NotNull PacketEntityMetadataState.Codec<
                SynchedEntityData,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor,
                Packet<? super ClientGamePacketListener>> createMetadataCodec() {
            return new PacketEntityMetadataState.Codec<>(
                    () -> new PacketEntityDataEditor(nmsEntity.getEntityData()),
                    SynchedEntityData.DataValue::id,
                    entries -> new ClientboundSetEntityDataPacket(nmsEntity.getId(), entries),
                    packet -> packet instanceof ClientboundSetEntityDataPacket metadata
                            && metadata.id() == nmsEntity.getId()
                            ? metadata.packedItems()
                            : null);
        }

        private @Nullable EnumMap<EquipmentSlot, ItemStack> createEquipmentSnapshot() {
            if (!(nmsEntity instanceof LivingEntity livingEntity))
                return null;
            EnumMap<EquipmentSlot, ItemStack> snapshot = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : EquipmentSlot.VALUES)
                snapshot.put(slot, livingEntity.getItemBySlot(slot).copy());
            return snapshot;
        }

        private @NotNull ServerLevel level() {
            return (ServerLevel)nmsEntity.level();
        }

        private @NotNull ServerEntity createServerEntity() {
            return serverEntityConstructor.invoke(
                    level(),
                    nmsEntity,
                    1,
                    nmsEntity.getType().trackDeltas(),
                    createServerEntitySynchronizer(),
                    Set.of());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private @NotNull Object createServerEntitySynchronizer() {
            return serverEntitySynchronizerClass
                    .<Object>map(synchronizerClass -> Proxy.newProxyInstance(synchronizerClass.getClassLoader(), new Class<?>[]{synchronizerClass}, (proxy, method, arguments) -> switch (method.getName()) {
                        case "sendToTrackingPlayers", "sendToTrackingPlayersAndSelf" -> {
                            tracker.broadcast((Packet)arguments[0]);
                            yield null;
                        }
                        case "sendToTrackingPlayersFiltered" -> {
                            tracker.broadcast((Packet)arguments[0], (Predicate)arguments[1]);
                            yield null;
                        }
                        case "equals" -> proxy == arguments[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "Paper packet entity synchronizer";
                        default -> throw new UnsupportedOperationException(method.toString());
                    }))
                    .orElseGet(() -> (Consumer<Packet<?>>)packet -> tracker.broadcast((Packet)packet));
        }

        private @NotNull Location location() {
            return new Location(
                    level().getWorld(),
                    nmsEntity.getX(),
                    nmsEntity.getY(),
                    nmsEntity.getZ(),
                    nmsEntity.getYRot(),
                    nmsEntity.getXRot());
        }

        private void move(@NotNull Location location) {
            ServerLevel newLevel = ((CraftWorld)location.getWorld()).getHandle();
            if (newLevel != level()) {
                nmsEntity.setLevel(newLevel);
                moveEntity(nmsEntity, location);
                serverEntity = createServerEntity();
                pendingRebind = true;
                return;
            }
            moveEntity(nmsEntity, location);
        }

        @Override
        public void synchronize(@NotNull PacketEntityTrackingCache<ServerLevel, ChunkPos, ServerPlayer> cache) {
            if (nmsEntity.isRemoved()) {
                entities.detach(apiEntity, this);
                tracker.close();
                return;
            }

            ServerLevel level = level();
            ServerLevel loadedLevel = level.getServer().getLevel(level.dimension());
            if (loadedLevel == null) {
                tracker.clearAudience();
                return;
            }
            if (pendingRebind || loadedLevel != level) {
                tracker.clearAudience();
                if (loadedLevel != level) {
                    nmsEntity.setLevel(loadedLevel);
                    serverEntity = createServerEntity();
                }
                pendingRebind = false;
                level = level();
            }

            int range = trackingRange();
            if (range <= 0) {
                tracker.synchronize(List.of(), range);
                return;
            }
            ServerLevel currentLevel = level;
            ChunkPos chunk = nmsEntity.chunkPosition();
            var players = cache.get(currentLevel, chunk, () -> currentLevel.getChunkSource().chunkMap.getPlayers(chunk, false));
            tracker.synchronize(players, range);
        }

        private int trackingRange() {
            return level().getServer().getScaledTrackingDistance(Math.max(0, trackingRange));
        }

        @Override
        public boolean canTrack(@NotNull ServerPlayer player, int range) {
            if (!source.isVisible(player.getUUID()) || player.isRemoved() || ((net.minecraft.world.entity.Entity)player).level() != level())
                return false;

            org.bukkit.entity.Player bukkitPlayer = player.getBukkitEntity();
            int playerRange = Math.min(range, bukkitPlayer.getSendViewDistance() * 16);
            double dx = player.getX() - nmsEntity.getX();
            double dz = player.getZ() - nmsEntity.getZ();
            return dx * dx + dz * dz <= (double)playerRange * playerRange
                    && bukkitPlayer.canSee(apiEntity)
                    && nmsEntity.broadcastToPlayer(player);
        }

        @Override
        public void addPairing(@NotNull ServerPlayer player) {
            ArrayList<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            serverEntity.sendPairingData(player, packets::add);
            packets = batch.flatten(packets);
            metadataState.pairing(player, packets);
            batch.sendLeaves(player, packets);
        }

        @Override
        public void removePairing(@NotNull ServerPlayer player) {
            batch.send(player, new ClientboundRemoveEntitiesPacket(nmsEntity.getId()));
            metadataState.forget(player);
        }

        @Override
        public @NotNull Packet<? super ClientGamePacketListener> personalize(@NotNull ServerPlayer player, @NotNull Packet<? super ClientGamePacketListener> packet) {
            return metadataState.personalize(player, packet);
        }

        @Override
        public void collectStatePackets() {
            if (equipment != null) {
                LivingEntity livingEntity = (LivingEntity)nmsEntity;
                ArrayList<Pair<EquipmentSlot, ItemStack>> changes = null;
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    ItemStack current = livingEntity.getItemBySlot(slot);
                    if (ItemStack.matches(equipment.get(slot), current))
                        continue;
                    ItemStack snapshot = current.copy();
                    equipment.put(slot, snapshot);
                    if (changes == null)
                        changes = new ArrayList<>();
                    changes.add(Pair.of(slot, snapshot));
                }
                if (changes != null)
                    tracker.broadcast(new ClientboundSetEquipmentPacket(nmsEntity.getId(), changes));
            }
            serverEntity.sendChanges();
        }

        @Override
        public void onClose() {
            nmsEntity.discard();
        }

        private void interact(@NotNull ServerPlayer player, @NotNull PlayerUseUnknownEntityEvent event) {
            if (!source.hasInteractionListeners())
                return;
            source.interact(player, interaction(event));
        }

        @Override
        public void close() {
            tracker.close();
        }

    }
}
