package org.lime.core.fabric.commands.brigadier.mixin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.platform.fabric.impl.accessor.brigadier.builder.RequiredArgumentBuilderAccess;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Redirect(
            method = "fillUsableCommands(Lcom/mojang/brigadier/tree/CommandNode;Lcom/mojang/brigadier/tree/CommandNode;Lnet/minecraft/commands/CommandSourceStack;Ljava/util/Map;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;build()Lcom/mojang/brigadier/tree/CommandNode;")
    )
    private CommandNode<SharedSuggestionProvider> argumentBuilder(
            ArgumentBuilder<SharedSuggestionProvider, ?> argumentBuilder) {
        if (argumentBuilder instanceof RequiredArgumentBuilder<?,?> builder) {
            if (builder.getType() instanceof CustomArgumentType<?,?> customArgumentType) {
                ((RequiredArgumentBuilderAccess) builder).accessor$type(customArgumentType.mappedArgument().nativeType());
                if (builder.getSuggestionsProvider() == null)
                    builder.suggests(customArgumentType::listSuggestions);
            }
        }
        return argumentBuilder.build();
    }
}
