package org.lime.core.fabric.commands.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.execute.Action2;

public class CommandHelperUtils {
    @SuppressWarnings("unchecked")
    private static final Action2<RequiredArgumentBuilder<?,?>, ArgumentType<?>> typeSetter = ReflectionField.of(RequiredArgumentBuilder.class, "type")
            .nonFinal()
            .setter(Action2.class);

    public static <T> CommandNode<T> argumentBuilder(
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
