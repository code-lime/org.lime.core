package net.minecraft.world.entity.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityTrackingRangeEvent
        extends EntityEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected int trackingRange;

    public EntityTrackingRangeEvent(
            final Entity entity,
            final int trackingRange) {
        super(entity);
        this.trackingRange = trackingRange;
    }

    public static int execute(int trackingRange, Entity entity) {
        EntityTrackingRangeEvent event = new EntityTrackingRangeEvent(entity, trackingRange);
        Bukkit.getPluginManager().callEvent(event);
        return event.trackingRange;
    }
    public static int execute(int trackingRange, net.minecraft.world.entity.Entity entity) {
        return execute(trackingRange, entity.getBukkitEntity());
    }

    public int trackingRange() {
        return trackingRange;
    }
    public void trackingRange(int trackingRange) {
        this.trackingRange = trackingRange;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
