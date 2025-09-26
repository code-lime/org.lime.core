package net.minecraft.world.entity.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class ShouldEntityBeSavedEvent
        extends EntityEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected boolean saved;

    public ShouldEntityBeSavedEvent(
            final Entity entity,
            final boolean saved) {
        super(entity);
        this.saved = saved;
    }

    public static boolean execute(boolean saved, Entity entity) {
        ShouldEntityBeSavedEvent event = new ShouldEntityBeSavedEvent(entity, !saved);
        Bukkit.getPluginManager().callEvent(event);
        return !event.saved;
    }
    public static boolean execute(boolean saved, net.minecraft.world.entity.Entity entity) {
        return execute(saved, entity.getBukkitEntity());
    }

    public boolean saved() {
        return saved;
    }
    public void saved(boolean saved) {
        this.saved = saved;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
