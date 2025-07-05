package org.lime.core.paper;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.api.BaseBrigadier;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;

import java.util.concurrent.CompletableFuture;

public interface PaperBrigadier
        extends BaseBrigadier {
    @Override
    default Message brigadierTooltip(Component component) {
        return new AdventureComponent(component);
    }
    @Override
    default <T, N>ArgumentType<T> brigadierArgument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType.Converted<T, N>() {
            @Override
            public @NotNull ArgumentType<N> getNativeType() {
                return mappedArgument.nativeType();
            }
            @Override
            public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
                return mappedArgument.suggestions(context, builder);
            }
            @Override
            public @NotNull T convert(@NotNull N value) throws CommandSyntaxException {
                return mappedArgument.convert(value);
            }
            @Override
            public <S> @NotNull T convert(@NotNull N value, @NotNull S source) throws CommandSyntaxException {
                return mappedArgument.convert(value, source);
            }
        };
    }
}
