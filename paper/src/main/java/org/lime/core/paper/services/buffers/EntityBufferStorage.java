package org.lime.core.paper.services.buffers;

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

    public <T extends Entity> IterationEntityBuffer<T> entity(String tag, Class<T> tClass, World defaultWorld) {
        return new IterationEntityBuffer<>(this, tag, tClass, defaultWorld);
    }
    public <Index, T extends Entity> IndexedEntityBuffer<Index, T> entity(String tag, Class<Index> indexClass, Class<T> tClass, World defaultWorld) {
        return new IndexedEntityBuffer<>(this, tag, indexClass, tClass, defaultWorld);
    }

    public IterationEntityBuffer<TextDisplay> text(String tag, World defaultWorld) {
        return entity(tag, TextDisplay.class, defaultWorld);
    }
    public IterationEntityBuffer<ItemDisplay> item(String tag, World defaultWorld) {
        return entity(tag, ItemDisplay.class, defaultWorld);
    }
    public IterationEntityBuffer<BlockDisplay> block(String tag, World defaultWorld) {
        return entity(tag, BlockDisplay.class, defaultWorld);
    }
    public IterationEntityBuffer<Interaction> interact(String tag, World defaultWorld) {
        return entity(tag, Interaction.class, defaultWorld);
    }

    public <Index>IndexedEntityBuffer<Index, TextDisplay> text(String tag, Class<Index> indexClass, World defaultWorld) {
        return entity(tag, indexClass, TextDisplay.class, defaultWorld);
    }
    public <Index>IndexedEntityBuffer<Index, ItemDisplay> item(String tag, Class<Index> indexClass, World defaultWorld) {
        return entity(tag, indexClass, ItemDisplay.class, defaultWorld);
    }
    public <Index>IndexedEntityBuffer<Index, BlockDisplay> block(String tag, Class<Index> indexClass, World defaultWorld) {
        return entity(tag, indexClass, BlockDisplay.class, defaultWorld);
    }
    public <Index>IndexedEntityBuffer<Index, Interaction> interact(String tag, Class<Index> indexClass, World defaultWorld) {
        return entity(tag, indexClass, Interaction.class, defaultWorld);
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
        boolean exist = false;
        for (var buffer : buffers) {
            if (!buffer.hasEntity(e.getEntity()))
                continue;
            exist = true;
            break;
        }
        if (exist)
            e.saved(false);
    }
}
