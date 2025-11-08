package org.lime.core.fabric.mixin;

import net.minecraft.world.entity.Entity;
import org.lime.core.fabric.hooks.ShouldBeEntitySavedHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class ShouldBeEntitySavedMixin {
    @Inject(method = "shouldBeSaved", at = @At(value = "RETURN"), cancellable = true)
    public void shouldBeSaved(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ShouldBeEntitySavedHook.EVENT.invoker().shouldBeSaved(cir.getReturnValue(), (Entity) (Object)this));
    }
}
