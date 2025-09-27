package org.lime.core.paper.services.buffers;

import com.google.inject.Inject;
import net.minecraft.world.entity.events.EntityTrackingRangeEvent;
import net.minecraft.world.entity.events.ShouldEntityBeSavedEvent;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.lime.core.common.api.BindService;
import org.lime.core.common.api.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@BindService
public class EntityBufferStorage
        implements Service, Listener {
    final Set<BaseEntityBuffer<?,?>> buffers = ConcurrentHashMap.newKeySet();
    @Inject World defaultWorld;

    public <T extends Entity> IterationEntityBuffer<T> entity(EntityBufferSetup setup, Class<T> tClass) {
        return new IterationEntityBuffer<>(this, setup, tClass);
    }
    public <Index, T extends Entity> IndexedEntityBuffer<Index, T> entity(EntityBufferSetup setup, Class<Index> indexClass, Class<T> tClass) {
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
    public void unregister() {
        buffers.removeIf(v -> {
            v.close();
            return true;
        });
    }

    @EventHandler
    private void on(ChunkLoadEvent e) {
        for (Entity entity : e.getChunk().getEntities())
            buffers.forEach(buffer -> buffer.entityLoad(entity));
    }
    @EventHandler
    private void on(ShouldEntityBeSavedEvent e) {
        if (!e.saved())
            return;
        for (var buffer : buffers) {
            if (buffer.hasEntity(e.getEntity())) {
                e.saved(false);
                return;
            }
        }
    }
    @EventHandler
    private void on(EntityTrackingRangeEvent e) {
        for (var buffer : buffers) {
            if (buffer.hasEntity(e.getEntity())) {
                buffer.setup
                        .trackingDistance()
                        .ifPresent(e::trackingRange);
                return;
            }
        }
    }
}
