package org.lime.core.fabric.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
//#switch PROPERTIES.versionMinecraft
//#caseofregex ^1\.(20\.1|21\.[0-9]+)$
//#default
//OF//import net.minecraft.network.protocol.game.ServerboundAttackPacket;
//#endswitch
import org.lime.core.common.services.buffers.PacketEntityInteraction;
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
    //#switch PROPERTIES.versionMinecraft
    //#caseofregex ^1\.(20\.1|21\.[0-9]+)$
    //#default
    //OF//    @Inject(method = "handleAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER), cancellable = true)
    //OF//    private void attackPacketEntity(@NotNull ServerboundAttackPacket packet, @NotNull CallbackInfo callback) {
    //OF//        if (PacketEntityInteractionHook.interact(player, packet.entityId(), PacketEntityInteraction.attack(player.isShiftKeyDown())))
    //OF//            callback.cancel();
    //OF//    }
    //#endswitch
    @Shadow public @NotNull ServerPlayer player;

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V", shift = At.Shift.AFTER), cancellable = true)
    private void interactPacketEntity(@NotNull ServerboundInteractPacket packet, @NotNull CallbackInfo callback) {
        //#switch PROPERTIES.versionMinecraft
        //#caseofregex ^1\.(20\.1|21\.[0-9]+)$
        int entityId = ((ServerboundInteractPacketAccessor)packet).lime$getEntityId();
        //#default
        //OF//        int entityId = packet.entityId();
        //#endswitch
        if (PacketEntityInteractionHook.interact(player, entityId, PacketEntityInteractionHook.decode(packet)))
            callback.cancel();
    }
}
