package org.lime.core.fabric.hooks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

public interface ShouldBeEntitySavedHook {
    Event<ShouldBeEntitySavedHook> EVENT = EventFactory.createArrayBacked(ShouldBeEntitySavedHook.class,
            (hooks) -> (saved, entity) -> {
                for (ShouldBeEntitySavedHook hook : hooks)
                    saved = hook.shouldBeSaved(saved, entity);
                return saved;
            });

    boolean shouldBeSaved(boolean saved, Entity entity);
}
