package org.lime.core.paper.services.buffers;

import com.google.inject.Inject;
import net.minecraft.world.entity.events.EntityTrackingRangeEvent;
import net.minecraft.world.entity.events.ShouldEntityBeSavedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseEntityBufferStorage;
import org.lime.core.common.utils.execute.Action1;

import java.util.Arrays;
import java.util.Set;

@BindService
public class EntityBufferStorage
        extends BaseEntityBufferStorage<Entity, Location>
        implements Listener {
    @Inject World defaultWorld;

    @EventHandler
    private void on(ChunkLoadEvent e) {
        onLoaded(Arrays.asList(e.getChunk().getEntities()));
    }
    @EventHandler
    private void on(ShouldEntityBeSavedEvent e) {
        e.saved(isShouldBeSave(e.saved(), e.getEntity()));
    }
    @EventHandler
    private void on(EntityTrackingRangeEvent e) {
        getTrackingRange(e.getEntity()).ifPresent(e::trackingRange);
    }

    @Override
    public <T extends Entity> IterationEntityBuffer<T> entity(BaseEntityBufferSetup<Location> setup, Class<T> tClass) {
        return new IterationEntityBuffer<>(this, setup, tClass);
    }
    @Override
    public <Index, T extends Entity> IndexedEntityBuffer<Index, T> entity(BaseEntityBufferSetup<Location> setup, Class<Index> indexClass, Class<T> tClass) {
        return new IndexedEntityBuffer<>(this, setup, indexClass, tClass);
    }

    public IterationEntityBuffer<TextDisplay> text(EntityBufferSetup setup) {
        return entity(setup, TextDisplay.class);
    }
    public IterationEntityBuffer<ItemDisplay> item(EntityBufferSetup setup) {
        return entity(setup, ItemDisplay.class);
    }
    public IterationEntityBuffer<BlockDisplay> block(EntityBufferSetup setup) {
        return entity(setup, BlockDisplay.class);
    }
    public IterationEntityBuffer<Interaction> interact(EntityBufferSetup setup) {
        return entity(setup, Interaction.class);
    }

    public <Index>IndexedEntityBuffer<Index, TextDisplay> text(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, TextDisplay.class);
    }
    public <Index>IndexedEntityBuffer<Index, ItemDisplay> item(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, ItemDisplay.class);
    }
    public <Index>IndexedEntityBuffer<Index, BlockDisplay> block(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, BlockDisplay.class);
    }
    public <Index>IndexedEntityBuffer<Index, Interaction> interact(EntityBufferSetup setup, Class<Index> indexClass) {
        return entity(setup, indexClass, Interaction.class);
    }

    @Override
    protected Location defaultLocation() {
        return new Location(defaultWorld, 0, 0, 0);
    }

    @Override
    protected <T extends Entity> T spawn(Location location, Class<T> entityClass, Action1<T> setup) {
        return location.getWorld().spawn(location, entityClass, setup);
    }
    @Override
    protected void remove(Entity entity) {
        entity.remove();
    }
    @Override
    protected void forEntities(Action1<Entity> consumer) {
        Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(consumer));
    }

    @Override
    protected Set<String> getTags(Entity v) {
        return v.getScoreboardTags();
    }
    @Override
    protected boolean isValid(Entity entity) {
        return entity.isValid();
    }
    @Override
    protected Location getLocation(Entity entity) {
        return entity.getLocation();
    }
    @Override
    protected void teleport(Entity entity, Location location) {
        entity.teleport(location);
    }
}
