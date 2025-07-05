package org.lime.core.common.api.commands.brigadier.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public interface BaseMappedArgument<T, N> {
    ArgumentType<N> nativeType();
    T convert(N value) throws CommandSyntaxException;
    default <S>T convert(N value, S source) throws CommandSyntaxException {
        return convert(value);
    }
    default <S> CompletableFuture<Suggestions> suggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return Suggestions.empty();
    }
}
