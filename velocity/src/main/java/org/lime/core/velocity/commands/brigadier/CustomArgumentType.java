package org.lime.core.velocity.commands.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;

import java.util.concurrent.CompletableFuture;

public record CustomArgumentType<T, N>(
        BaseMappedArgument<T, N> mappedArgument)
        implements ArgumentType<T> {
    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        return mappedArgument.convert(mappedArgument.nativeType().parse(reader));
    }
    public <S>T parse(StringReader reader, S source) throws CommandSyntaxException {
        return mappedArgument.convert(mappedArgument.nativeType().parse(reader), source);
    }
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return mappedArgument.suggestions(context, builder);
    }
}
