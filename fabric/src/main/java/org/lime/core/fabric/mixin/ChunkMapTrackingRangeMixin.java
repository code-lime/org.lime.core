package org.lime.core.fabric.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.lime.core.fabric.hooks.EntityTrackingRangeHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkMap.class)
public class ChunkMapTrackingRangeMixin {
    @ModifyVariable(method = "addEntity", at = @At(value = "STORE"), ordinal = 0)
    private int modifyTrackingRange(int i, Entity entity) {
        return EntityTrackingRangeHook.EVENT.invoker().trackingRange(i, entity);
    }
}
