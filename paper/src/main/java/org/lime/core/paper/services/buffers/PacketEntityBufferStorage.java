package org.lime.core.paper.services.buffers;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.kyori.adventure.key.Key;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
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
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BasePacketEntityBufferStorage;
import org.lime.core.common.services.buffers.InjectBuffer;
import org.lime.core.common.services.buffers.PacketEntityViewSource;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action6;
import org.lime.core.common.utils.execute.Func6;
import org.slf4j.Logger;
import org.spigotmc.TrackingRange;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Owns detached NMS entities and mirrors their state to nearby players through
 * a standalone {@link ServerEntity}. The entities are never added to a world.
 */
@BindService
public class PacketEntityBufferStorage
        extends BasePacketEntityBufferStorage<Entity, Location> {
    private interface PacketSynchronizer {
        void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet);
        void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet);
        void sendToTrackingPlayersFiltered(
                Packet<? super ClientGamePacketListener> packet,
                Predicate<ServerPlayer> filter);
    }

    private static final Optional<Class<?>> serverEntitySynchronizerClass = Reflection.findClassOptional("net.minecraft.server.level.ServerEntity$Synchronizer");
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
            ServerEntity> serverEntityConstructor = ReflectionConstructor.of(
                    ServerEntity.class,
                    ServerLevel.class,
                    net.minecraft.world.entity.Entity.class,
                    int.class,
                    boolean.class,
                    serverEntitySynchronizerClass.orElse(Consumer.class),
                    Set.class)
            .lambda(Func6.class);

    private static void moveEntity(
            net.minecraft.world.entity.Entity entity,
            Location location) {
        moveEntity.invoke(
                entity,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object createServerEntitySynchronizer(PacketSynchronizer synchronizer) {
        return serverEntitySynchronizerClass
                .<Object>map(synchronizerClass -> Proxy.newProxyInstance(
                        synchronizerClass.getClassLoader(),
                        new Class<?>[]{synchronizerClass},
                        (proxy, method, arguments) -> switch (method.getName()) {
                            case "sendToTrackingPlayers" -> {
                                synchronizer.sendToTrackingPlayers((Packet)arguments[0]);
                                yield null;
                            }
                            case "sendToTrackingPlayersAndSelf" -> {
                                synchronizer.sendToTrackingPlayersAndSelf((Packet)arguments[0]);
                                yield null;
                            }
                            case "sendToTrackingPlayersFiltered" -> {
                                synchronizer.sendToTrackingPlayersFiltered(
                                        (Packet)arguments[0],
                                        (Predicate)arguments[1]);
                                yield null;
                            }
                            case "equals" -> proxy == arguments[0];
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "toString" -> "Paper ServerEntity synchronizer";
                            default -> throw new UnsupportedOperationException(method.toString());
                        }))
                .orElseGet(() -> (Consumer<Packet<?>>)packet ->
                        synchronizer.sendToTrackingPlayers((Packet)packet));
    }

    private final ConcurrentHashMap<Integer, PacketEntityHandle> entities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PacketEntityViewSource<
            Player,
            EntityDataAccessor<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor>> pendingViewSources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends Entity>, Optional<EntityType>> apiEntityTypes = new ConcurrentHashMap<>();
    private final TrackingPlayers trackingPlayers = new TrackingPlayers();

    @Inject World defaultWorld;
    @Inject ScheduleTaskService taskService;
    @Inject Logger logger;

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
        pendingViewSources.clear();
        trackingPlayers.close();
    }

    private void tick() {
        trackingPlayers.reset();
        entities.values().forEach(handle -> handle.tick(trackingPlayers));
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

        moveEntity(nmsEntity, location);

        T apiEntity;
        try {
            apiEntity = entityClass.cast(nmsEntity.getBukkitEntity());
            setup.invoke(apiEntity);
        } catch (RuntimeException | Error exception) {
            pendingViewSources.remove(nmsEntity.getId());
            nmsEntity.discard();
            throw exception;
        }

        int trackingRange = getTrackingRange(apiEntity).orElseGet(() -> TrackingRange.getEntityTrackingRange(
                nmsEntity,
                nmsType.clientTrackingRange() * 16));
        PacketEntityHandle handle = new PacketEntityHandle(
                apiEntity,
                nmsEntity,
                trackingRange,
                pendingViewSources.remove(nmsEntity.getId()));
        PacketEntityHandle previous = entities.putIfAbsent(nmsEntity.getId(), handle);
        if (previous != null) {
            nmsEntity.discard();
            throw new IllegalStateException("Duplicate packet entity id " + nmsEntity.getId());
        }
        return apiEntity;
    }

    @Override
    protected void remove(Entity entity) {
        pendingViewSources.remove(entity.getEntityId());
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

    void attachView(
            Entity entity,
            PacketEntityViewSource<
                    Player,
                    EntityDataAccessor<?>,
                    SynchedEntityData.DataValue<?>,
                    PacketEntityDataEditor> source) {
        Objects.requireNonNull(source, "source");
        PacketEntityHandle handle = getHandle(entity);
        if (handle != null) {
            handle.attachView(source);
            return;
        }
        pendingViewSources.put(entity.getEntityId(), source);
    }

    void refreshViews(
            Iterable<? extends Entity> entities,
            @Nullable EntityDataAccessor<?> trigger,
            @Nullable Player player) {
        entities.forEach(entity -> {
            PacketEntityHandle handle = getHandle(entity);
            if (handle == null || trigger != null && !handle.isTriggered(trigger))
                return;
            handle.refreshViews(player);
        });
    }

    void refreshTracking(Iterable<? extends Entity> trackedEntities) {
        TrackingPlayers players = new TrackingPlayers();
        trackedEntities.forEach(entity -> {
            PacketEntityHandle handle = getHandle(entity);
            if (handle != null)
                handle.updateViewers(players);
        });
    }

    void requireServerThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Packet entity view API must be used on the server thread");
    }

    private static final class TrackingPlayers {
        private final Reference2ObjectOpenHashMap<
                ServerLevel,
                Object2ObjectOpenHashMap<ChunkPos, ReferenceOpenHashSet<ServerPlayer>>> values =
                new Reference2ObjectOpenHashMap<>(4);
        private final ArrayDeque<Object2ObjectOpenHashMap<ChunkPos, ReferenceOpenHashSet<ServerPlayer>>> reusable =
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

        private ReferenceOpenHashSet<ServerPlayer> get(ServerLevel level, ChunkPos chunk) {
            Object2ObjectOpenHashMap<ChunkPos, ReferenceOpenHashSet<ServerPlayer>> chunks = values.get(level);
            if (chunks == null) {
                chunks = reusable.pollFirst();
                if (chunks == null)
                    chunks = new Object2ObjectOpenHashMap<>();
                values.put(level, chunks);
            }

            ReferenceOpenHashSet<ServerPlayer> players = chunks.get(chunk);
            if (players == null) {
                var source = level.getChunkSource().chunkMap.getPlayers(chunk, false);
                players = new ReferenceOpenHashSet<>(source.size());
                players.addAll(source);
                chunks.put(chunk, players);
            }
            return players;
        }
    }

    private static final CanonicalData EMPTY_CANONICAL = new CanonicalData(
            List.of(),
            Int2ObjectMaps.emptyMap());

    private record CanonicalData(
            List<SynchedEntityData.DataValue<?>> values,
            Int2ObjectMap<SynchedEntityData.DataValue<?>> byId) {}

    private record ViewComputation(
            CanonicalData canonical,
            Map<Integer, SynchedEntityData.DataValue<?>> overlay) {}

    private final class PacketEntityHandle
            implements PacketSynchronizer {
        private final Entity apiEntity;
        private final net.minecraft.world.entity.Entity nmsEntity;
        private final int trackingRange;
        private final Set<ServerPlayerConnection> viewers = new ReferenceOpenHashSet<>(4);
        private final Set<ServerPlayerConnection> desiredViewers = new ReferenceOpenHashSet<>(4);
        private final @Nullable EnumMap<EquipmentSlot, ItemStack> equipmentSnapshot;
        private final Map<UUID, Map<Integer, SynchedEntityData.DataValue<?>>> viewOverlays = new HashMap<>();

        private ServerEntity serverEntity;
        private @Nullable PacketEntityViewSource<
                Player,
                EntityDataAccessor<?>,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor> viewSource;
        private boolean removed;

        private PacketEntityHandle(
                Entity apiEntity,
                net.minecraft.world.entity.Entity nmsEntity,
                int trackingRange,
                @Nullable PacketEntityViewSource<
                        Player,
                        EntityDataAccessor<?>,
                        SynchedEntityData.DataValue<?>,
                        PacketEntityDataEditor> viewSource) {
            this.apiEntity = apiEntity;
            this.nmsEntity = nmsEntity;
            this.trackingRange = trackingRange;
            this.viewSource = viewSource;
            this.equipmentSnapshot = createEquipmentSnapshot();
            this.serverEntity = createServerEntity();
        }

        private void attachView(PacketEntityViewSource<
                Player,
                EntityDataAccessor<?>,
                SynchedEntityData.DataValue<?>,
                PacketEntityDataEditor> viewSource) {
            this.viewSource = viewSource;
            updateViewers(new TrackingPlayers());
            refreshViews(null);
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
            return serverEntityConstructor.invoke(
                    level(),
                    nmsEntity,
                    type.updateInterval(),
                    type.trackDeltas(),
                    createServerEntitySynchronizer(this),
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
                moveEntity(nmsEntity, location);
                serverEntity = createServerEntity();
                return;
            }

            moveEntity(nmsEntity, location);
        }

        private int trackingRange() {
            return level().getServer().getScaledTrackingDistance(Math.max(0, trackingRange));
        }

        private void tick(TrackingPlayers trackingPlayers) {
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

            updateViewers(trackingPlayers);
            sendEquipmentChanges();
            serverEntity.sendChanges();
        }

        private void updateViewers(TrackingPlayers trackingPlayers) {
            desiredViewers.clear();
            int range = trackingRange();
            var chunk = nmsEntity.chunkPosition();
            if (range > 0) {
                ServerLevel level = level();
                var trackedPlayers = trackingPlayers.get(level, chunk);
                PacketEntityVisibility visibility = visibility();
                if (visibility.defaultVisible()) {
                    for (ServerPlayer player : trackedPlayers) {
                        if (visibility.isVisible(player.getUUID()))
                            addCandidate(player, range);
                    }
                } else {
                    for (UUID playerId : visibility.exceptions()) {
                        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
                        if (player != null
                                && trackedPlayers.contains(player)
                                && visibility.isVisible(playerId))
                            addCandidate(player, range);
                    }
                }
            }

            viewers.removeIf(connection -> {
                if (desiredViewers.remove(connection))
                    return false;
                serverEntity.removePairing(connection.getPlayer());
                viewOverlays.remove(connection.getPlayer().getUUID());
                return true;
            });
            desiredViewers.forEach(connection -> {
                if (!viewers.add(connection))
                    return;
                ServerPlayer player = connection.getPlayer();
                addPairing(connection, player);
                serverEntity.onPlayerAdd();
            });
            desiredViewers.clear();
        }

        private void addCandidate(ServerPlayer player, int range) {
            if (player.isRemoved())
                return;

            Player bukkitPlayer = player.getBukkitEntity();
            int playerRange = Math.min(range, bukkitPlayer.getSendViewDistance() * 16);
            double dx = player.getX() - nmsEntity.getX();
            double dz = player.getZ() - nmsEntity.getZ();
            if (dx * dx + dz * dz > (double)playerRange * playerRange)
                return;
            if (!bukkitPlayer.canSee(apiEntity))
                return;

            if (nmsEntity.broadcastToPlayer(player))
                desiredViewers.add(player.connection);
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

        private void addPairing(ServerPlayerConnection connection, ServerPlayer player) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            serverEntity.sendPairingData(player, packets::add);

            ViewComputation computation = computeView(player);
            if (computation == null) {
                viewOverlays.remove(player.getUUID());
                connection.send(new ClientboundBundlePacket(packets));
                nmsEntity.startSeenByPlayer(player);
                return;
            }

            storeOverlay(player.getUUID(), computation.overlay());
            if (computation.overlay().isEmpty()) {
                connection.send(new ClientboundBundlePacket(packets));
                nmsEntity.startSeenByPlayer(player);
                return;
            }

            int metadataIndex = -1;
            for (int i = 0; i < packets.size(); i++) {
                Packet<? super ClientGamePacketListener> packet = packets.get(i);
                if (!(packet instanceof ClientboundSetEntityDataPacket metadata)
                        || metadata.id() != nmsEntity.getId())
                    continue;
                if (metadataIndex < 0)
                    metadataIndex = i;
                packets.remove(i--);
            }

            if (metadataIndex < 0)
                metadataIndex = Math.min(1, packets.size());
            packets.add(
                    Math.min(metadataIndex, packets.size()),
                    new ClientboundSetEntityDataPacket(
                            nmsEntity.getId(),
                            mergeCanonical(computation.canonical().values(), computation.overlay())));
            connection.send(new ClientboundBundlePacket(packets));
            nmsEntity.startSeenByPlayer(player);
        }

        private @Nullable ViewComputation computeView(ServerPlayer player) {
            return computeView(player, null);
        }

        private @Nullable ViewComputation computeView(
                ServerPlayer player,
                @Nullable CanonicalData canonical) {
            PacketEntityViewSource<
                    Player,
                    EntityDataAccessor<?>,
                    SynchedEntityData.DataValue<?>,
                    PacketEntityDataEditor> source = viewSource;
            if (source == null || !source.hasListeners())
                return new ViewComputation(EMPTY_CANONICAL, Map.of());

            if (canonical == null)
                canonical = canonicalData();
            PacketEntityDataEditor editor = new PacketEntityDataEditor(canonical.byId());
            try {
                source.edit((Player)player.getBukkitEntity(), editor);
                return new ViewComputation(canonical, editor.snapshot());
            } catch (RuntimeException exception) {
                logger.error(
                        "Unable to compute packet entity metadata view for entity {} and player {}",
                        nmsEntity.getId(),
                        player.getUUID(),
                        exception);
                return null;
            }
        }

        private void refreshViews(@Nullable Player target) {
            UUID targetId = target == null ? null : target.getUniqueId();
            CanonicalData canonical = null;
            for (ServerPlayerConnection connection : new ArrayList<>(viewers)) {
                ServerPlayer player = connection.getPlayer();
                if (targetId != null && !targetId.equals(player.getUUID()))
                    continue;
                if (canonical == null) {
                    PacketEntityViewSource<
                            Player,
                            EntityDataAccessor<?>,
                            SynchedEntityData.DataValue<?>,
                            PacketEntityDataEditor> source = viewSource;
                    canonical = source == null || !source.hasListeners()
                            ? EMPTY_CANONICAL
                            : canonicalData();
                }
                refreshView(connection, player, canonical);
            }
        }

        private void refreshView(
                ServerPlayerConnection connection,
                ServerPlayer player,
                CanonicalData sharedCanonical) {
            ViewComputation computation = computeView(player, sharedCanonical);
            if (computation == null)
                return;

            Map<Integer, SynchedEntityData.DataValue<?>> oldOverlay = overlay(player.getUUID());
            CanonicalData canonical = computation.canonical();
            if (canonical.values().isEmpty() && !oldOverlay.isEmpty())
                canonical = canonicalData();
            List<SynchedEntityData.DataValue<?>> delta = overlayDelta(
                    oldOverlay,
                    computation.overlay(),
                    canonical.byId());
            storeOverlay(player.getUUID(), computation.overlay());
            if (!delta.isEmpty())
                connection.send(new ClientboundSetEntityDataPacket(nmsEntity.getId(), delta));
        }

        private @Nullable Packet<? super ClientGamePacketListener> personalize(
                ServerPlayer player,
                Packet<? super ClientGamePacketListener> packet,
                @Nullable CanonicalData sharedCanonical) {
            if (!(packet instanceof ClientboundSetEntityDataPacket metadata)
                    || metadata.id() != nmsEntity.getId())
                return packet;

            UUID playerId = player.getUUID();
            Map<Integer, SynchedEntityData.DataValue<?>> oldOverlay = overlay(playerId);
            Map<Integer, SynchedEntityData.DataValue<?>> currentOverlay = oldOverlay;
            List<SynchedEntityData.DataValue<?>> overlayDelta = List.of();

            boolean recompute = sharedCanonical != null;
            if (!recompute && oldOverlay.isEmpty())
                return packet;

            if (recompute) {
                ViewComputation computation = computeView(player, sharedCanonical);
                if (computation != null) {
                    currentOverlay = computation.overlay();
                    overlayDelta = overlayDelta(
                            oldOverlay,
                            currentOverlay,
                            computation.canonical().byId());
                    storeOverlay(playerId, currentOverlay);
                }
            }

            if (currentOverlay.isEmpty() && overlayDelta.isEmpty())
                return packet;

            if (overlayDelta.isEmpty()) {
                boolean hasOverriddenValue = false;
                for (SynchedEntityData.DataValue<?> value : metadata.packedItems()) {
                    if (currentOverlay.containsKey(value.id())) {
                        hasOverriddenValue = true;
                        break;
                    }
                }
                if (!hasOverriddenValue)
                    return packet;
            }

            Int2ObjectLinkedOpenHashMap<SynchedEntityData.DataValue<?>> personalized =
                    new Int2ObjectLinkedOpenHashMap<>(
                            metadata.packedItems().size() + overlayDelta.size());
            for (SynchedEntityData.DataValue<?> value : metadata.packedItems())
                personalized.put(value.id(), currentOverlay.getOrDefault(value.id(), value));
            for (SynchedEntityData.DataValue<?> value : overlayDelta)
                personalized.put(value.id(), value);

            return personalized.isEmpty()
                    ? null
                    : new ClientboundSetEntityDataPacket(metadata.id(), List.copyOf(personalized.values()));
        }

        private @Nullable CanonicalData viewCanonical(
                Packet<? super ClientGamePacketListener> packet) {
            if (!(packet instanceof ClientboundSetEntityDataPacket metadata)
                    || metadata.id() != nmsEntity.getId())
                return null;

            PacketEntityViewSource<
                    Player,
                    EntityDataAccessor<?>,
                    SynchedEntityData.DataValue<?>,
                    PacketEntityDataEditor> source = viewSource;
            return source != null
                    && source.hasListeners()
                    && metadata.packedItems().stream().anyMatch(source::isTriggeredUpdate)
                    ? canonicalData()
                    : null;
        }

        private Map<Integer, SynchedEntityData.DataValue<?>> overlay(UUID playerId) {
            return viewOverlays.getOrDefault(playerId, Map.of());
        }

        private void storeOverlay(
                UUID playerId,
                Map<Integer, SynchedEntityData.DataValue<?>> overlay) {
            if (overlay.isEmpty())
                viewOverlays.remove(playerId);
            else
                viewOverlays.put(playerId, overlay);
        }

        private List<SynchedEntityData.DataValue<?>> overlayDelta(
                Map<Integer, SynchedEntityData.DataValue<?>> oldOverlay,
                Map<Integer, SynchedEntityData.DataValue<?>> newOverlay,
                Int2ObjectMap<SynchedEntityData.DataValue<?>> canonicalById) {
            Int2ObjectLinkedOpenHashMap<SynchedEntityData.DataValue<?>> delta =
                    new Int2ObjectLinkedOpenHashMap<>(oldOverlay.size() + newOverlay.size());

            for (Integer id : oldOverlay.keySet()) {
                if (newOverlay.containsKey(id))
                    continue;
                SynchedEntityData.DataValue<?> reset = canonicalById.get(id.intValue());
                if (reset != null)
                    delta.put(id.intValue(), reset);
            }
            for (Map.Entry<Integer, SynchedEntityData.DataValue<?>> entry : newOverlay.entrySet()) {
                if (!Objects.equals(oldOverlay.get(entry.getKey()), entry.getValue()))
                    delta.put(entry.getKey().intValue(), entry.getValue());
            }
            return List.copyOf(delta.values());
        }

        private List<SynchedEntityData.DataValue<?>> mergeCanonical(
                List<SynchedEntityData.DataValue<?>> canonical,
                Map<Integer, SynchedEntityData.DataValue<?>> overlay) {
            ArrayList<SynchedEntityData.DataValue<?>> result = new ArrayList<>(canonical.size());
            for (SynchedEntityData.DataValue<?> value : canonical)
                result.add(overlay.getOrDefault(value.id(), value));
            return result;
        }

        private Int2ObjectOpenHashMap<SynchedEntityData.DataValue<?>> index(
                List<SynchedEntityData.DataValue<?>> values) {
            Int2ObjectOpenHashMap<SynchedEntityData.DataValue<?>> result =
                    new Int2ObjectOpenHashMap<>(values.size());
            for (SynchedEntityData.DataValue<?> value : values)
                result.put(value.id(), value);
            return result;
        }

        private CanonicalData canonicalData() {
            List<SynchedEntityData.DataValue<?>> values = nmsEntity.getEntityData().packAll();
            return new CanonicalData(values, index(values));
        }

        private void clearViewers() {
            viewers.forEach(connection -> serverEntity.removePairing(connection.getPlayer()));
            viewers.clear();
            desiredViewers.clear();
            viewOverlays.clear();
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
            if (viewers.isEmpty())
                return;
            CanonicalData canonical = viewCanonical(packet);
            viewers.forEach(connection -> {
                Packet<? super ClientGamePacketListener> personalized = personalize(
                        connection.getPlayer(),
                        packet,
                        canonical);
                if (personalized != null)
                    connection.send(personalized);
            });
        }

        @Override
        public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
            sendToTrackingPlayers(packet);
        }

        @Override
        public void sendToTrackingPlayersFiltered(
                Packet<? super ClientGamePacketListener> packet,
                Predicate<ServerPlayer> filter) {
            if (viewers.isEmpty())
                return;
            CanonicalData canonical = viewCanonical(packet);
            viewers.forEach(connection -> {
                ServerPlayer player = connection.getPlayer();
                if (!filter.test(player))
                    return;
                Packet<? super ClientGamePacketListener> personalized = personalize(player, packet, canonical);
                if (personalized != null)
                    connection.send(personalized);
            });
        }
    }
}
