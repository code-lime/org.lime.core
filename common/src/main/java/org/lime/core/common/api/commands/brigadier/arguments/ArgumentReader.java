package org.lime.core.common.api.commands.brigadier.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.lime.core.common.utils.execute.Func1;

public interface ArgumentReader<T, S> {
    T read(CommandContext<S> context, String parameter) throws CommandSyntaxException;

    default ArgumentAccess<T> access(CommandContext<S> context, String parameter) {
        return () -> read(context, parameter);
    }
    default <I>ArgumentReader<I, S> map(Func1<T, I> convert) {
        return (ctx, parameter) -> convert.invoke(read(ctx, parameter));
    }
}
