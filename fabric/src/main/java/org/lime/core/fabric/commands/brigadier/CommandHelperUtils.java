package org.lime.core.fabric.commands.brigadier;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.platform.fabric.impl.accessor.brigadier.builder.RequiredArgumentBuilderAccess;

public class CommandHelperUtils {
    public static <T> CommandNode<T> argumentBuilder(
            ArgumentBuilder<T, ?> argumentBuilder) {
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
