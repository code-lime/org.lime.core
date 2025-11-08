package org.lime.core.fabric.services.buffers;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseEntityBufferStorage;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.fabric.hooks.EntityTrackingRangeHook;
import org.lime.core.fabric.hooks.ShouldBeEntitySavedHook;
import org.lime.core.fabric.utils.WorldLocation;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@BindService
public class EntityBufferStorage
        extends BaseEntityBufferStorage<Entity, WorldLocation> {
    @Inject MinecraftServer server;
    @Inject ServerLevel overworld;

    private final ConcurrentHashMap<Class<? extends Entity>, EntityType<?>> entities = new ConcurrentHashMap<>();

    @Override
    public Disposable register() {
        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(dat -> {
            var entity = dat.getValue().create(overworld
                    //#switch PROPERTIES.versionMinecraft
                    //#caseof 1.21.4;1.21.8
                    //OF//                    , EntitySpawnReason.COMMAND
                    //#default
                    //#endswitch
            );
            if (entity == null)
                return;
            entities.put(entity.getClass(), dat.getValue());
        });
        ShouldBeEntitySavedHook.EVENT.register(this::isShouldBeSave);
        EntityTrackingRangeHook.EVENT.register((range, entity) -> getTrackingRange(entity).orElse(range));
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> onLoaded(Collections.singleton(entity)));
        return super.register();
    }
    @SuppressWarnings("unchecked")
    private <T extends Entity>EntityType<T> getEntityType(Class<T> entityClass) {
        var result = entities.get(entityClass);
        if (result == null)
            throw new IllegalArgumentException("Entity class "+entityClass+" not supported");
        return (EntityType<T>)result;
    }

    @Override
    public <T extends Entity> IterationEntityBuffer<T> entity(BaseEntityBufferSetup<WorldLocation> setup, Class<T> tClass) {
        return new IterationEntityBuffer<>(this, setup, tClass);
    }
    @Override
    public <Index, T extends Entity> IndexedEntityBuffer<Index, T> entity(BaseEntityBufferSetup<WorldLocation> setup, Class<Index> indexClass, Class<T> tClass) {
        return new IndexedEntityBuffer<>(this, setup, indexClass, tClass);
    }

    public IterationEntityBuffer<Display.TextDisplay> text(EntityBufferSetup setup) {
        return entity(setup, Display.TextDisplay.class);
    }
    public IterationEntityBuffer<Display.ItemDisplay> item(EntityBufferSetup setup) {
        return entity(setup, Display.ItemDisplay.class);
    }
    public IterationEntityBuffer<Display.BlockDisplay> block(EntityBufferSetup setup) {
        return entity(setup, Display.BlockDisplay.class);
    }
    public IterationEntityBuffer<Interaction> interact(EntityBufferSetup setup) {
        return entity(setup, Interaction.class);
    }

    public <Index>IndexedEntityBuffer<Index, Display.TextDisplay> text(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, Display.TextDisplay.class);
    }
    public <Index>IndexedEntityBuffer<Index, Display.ItemDisplay> item(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, Display.ItemDisplay.class);
    }
    public <Index>IndexedEntityBuffer<Index, Display.BlockDisplay> block(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, Display.BlockDisplay.class);
    }
    public <Index>IndexedEntityBuffer<Index, Interaction> interact(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, Interaction.class);
    }

    @Override
    protected WorldLocation defaultLocation() {
        return new WorldLocation(overworld.dimension(), Vec3.ZERO, Vec2.ZERO);
    }

    @Override
    protected <T extends Entity> T spawn(WorldLocation location, Class<T> entityClass, Action1<T> setup) {
        //ServerLevel serverLevel, @Nullable Consumer<T> consumer, BlockPos blockPos, EntitySpawnReason entitySpawnReason, boolean bl, boolean bl2
        return getEntityType(entityClass)
                .spawn(location.level(server),
                        //#switch PROPERTIES.versionMinecraft
                        //#caseof 1.21.4;1.21.8
                        //#default
                        null,
                        //#endswitch
                        entity -> {
                    var pos = location.position();
                    //#switch PROPERTIES.versionMinecraft
                    //#caseof 1.21.8
                    //OF//                    entity.snapTo(pos.x, pos.y, pos.z, location.yaw(), location.pitch());
                    //#default
                    entity.moveTo(pos.x, pos.y, pos.z, location.yaw(), location.pitch());
                    //#endswitch
                    setup.invoke(entity);
                }, location.blockPos(),
                        //#switch PROPERTIES.versionMinecraft
                        //#caseof 1.21.4;1.21.8
                        //OF//                        EntitySpawnReason.COMMAND
                        //#default
                        MobSpawnType.COMMAND
                        //#endswitch
                        , false, false);
    }
    @Override
    protected void remove(Entity entity) {
        entity.discard();
    }
    @Override
    protected void forEntities(Action1<Entity> consumer) {
        server.getAllLevels().forEach(level -> level.getAllEntities().forEach(consumer));
    }

    @Override
    protected Set<String> getTags(Entity v) {
        return v.getTags();
    }
    @Override
    protected boolean isValid(Entity entity) {
        return entity.isAlive() && entity.level().getEntity(entity.getId()) != null;
    }
    @Override
    protected WorldLocation getLocation(Entity entity) {
        return new WorldLocation(entity.level().dimension(), entity.position(), entity.getRotationVector());
    }
    @Override
    protected void teleport(Entity entity, WorldLocation location) {
        var pos = location.position();
        entity.teleportTo(location.level(server), pos.x, pos.y, pos.z,
                //#switch PROPERTIES.versionMinecraft
                //#caseof 1.21.4;1.21.8
                //OF//                Relative.ALL
                //#default
                RelativeMovement.ALL
                //#endswitch
                , location.yaw(), location.pitch()
                //#switch PROPERTIES.versionMinecraft
                //#caseof 1.21.4;1.21.8
                //OF//                , true
                //#default
                //#endswitch
                );
    }
}
