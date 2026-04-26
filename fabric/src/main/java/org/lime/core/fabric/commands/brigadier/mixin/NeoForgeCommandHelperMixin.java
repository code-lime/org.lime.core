package org.lime.core.fabric.commands.brigadier.mixin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import org.lime.core.fabric.commands.brigadier.CommandHelperUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.neoforged.neoforge.server.command.CommandHelper", remap = false)
public class NeoForgeCommandHelperMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(
            method = "toResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;build()Lcom/mojang/brigadier/tree/CommandNode;")
    )
    private static <T>CommandNode<T> argumentBuilder(
            ArgumentBuilder<T, ?> argumentBuilder) {
        return CommandHelperUtils.argumentBuilder(argumentBuilder);
    }
}
