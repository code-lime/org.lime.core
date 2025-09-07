package org.lime.core.fabric.commands.brigadier.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.system.execute.Action2;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraftforge.server.command.CommandHelper", remap = false)
public class ForgeCommandHelperMixin {
    @Unique
    private static final Action2<RequiredArgumentBuilder<?,?>, ArgumentType<?>> typeSetter = ReflectionField.of(RequiredArgumentBuilder.class, "type")
            .access()
            .nonFinal()
            .setter(Action2.class);

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(
            method = "toResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;build()Lcom/mojang/brigadier/tree/CommandNode;")
    )
    private static <T>CommandNode<T> argumentBuilder(
            ArgumentBuilder<T, ?> argumentBuilder) {
        if (argumentBuilder instanceof RequiredArgumentBuilder<?,?> builder) {
            if (builder.getType() instanceof CustomArgumentType<?,?> customArgumentType) {
                typeSetter.invoke(builder, customArgumentType.mappedArgument().nativeType());
                if (builder.getSuggestionsProvider() == null)
                    builder.suggests(customArgumentType::listSuggestions);
            }
        }
        return argumentBuilder.build();
    }
}
