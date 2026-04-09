package org.lime.core.common.api.commands.brigadier.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface ArgumentReader<T, S> {
    T read(CommandContext<S> context, String parameter) throws CommandSyntaxException;

    default ArgumentAccess<T> access(CommandContext<S> context, String parameter) {
        return () -> read(context, parameter);
    }
}
