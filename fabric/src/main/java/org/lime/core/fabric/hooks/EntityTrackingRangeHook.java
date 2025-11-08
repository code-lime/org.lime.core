package org.lime.core.fabric.hooks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

public interface EntityTrackingRangeHook {
    Event<EntityTrackingRangeHook> EVENT = EventFactory.createArrayBacked(EntityTrackingRangeHook.class,
            (hooks) -> (range, entity) -> {
                for (EntityTrackingRangeHook hook : hooks)
                    range = hook.trackingRange(range, entity);
                return range;
            });

    int trackingRange(int range, Entity entity);
}
