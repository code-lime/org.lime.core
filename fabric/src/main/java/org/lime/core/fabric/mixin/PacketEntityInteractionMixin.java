package org.lime.core.fabric.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.lime.core.fabric.hooks.PacketEntityInteractionHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PacketEntityInteractionMixin {
    @Shadow public @NotNull ServerPlayer player;

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V", shift = At.Shift.AFTER), cancellable = true)
    private void interactPacketEntity(@NotNull ServerboundInteractPacket packet, @NotNull CallbackInfo callback) {
        int entityId = ((ServerboundInteractPacketAccessor)packet).lime$getEntityId();
        if (PacketEntityInteractionHook.interact(player, entityId, packet))
            callback.cancel();
    }
}
