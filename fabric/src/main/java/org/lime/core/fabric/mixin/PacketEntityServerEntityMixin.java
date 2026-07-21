package org.lime.core.fabric.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.lime.core.fabric.hooks.PacketEntitySendHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ServerEntity.class)
public abstract class PacketEntityServerEntityMixin implements PacketEntitySendHook {
    @Unique private boolean lime$packetEntity;

    @Override
    public void lime$markPacketEntity() {
        lime$packetEntity = true;
    }

    @Redirect(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;players()Ljava/util/List;"))
    private @NotNull List<ServerPlayer> packetEntityPlayers(@NotNull ServerLevel level) {
        return lime$packetEntity ? List.of() : level.players();
    }
}
