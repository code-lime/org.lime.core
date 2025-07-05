package org.lime.core.velocity.brigadier;

import com.mojang.brigadier.*;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

class CustomArgumentCommandNode<S, T, N>
        extends ArgumentCommandNode<S, N> {
    private final CustomArgumentType<T, N> customType;
    public CustomArgumentCommandNode(String name, CustomArgumentType<T, N> type, Command<S> command, Predicate<S> requirement, BiPredicate<CommandContextBuilder<S>, ImmutableStringReader> contextRequirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks, SuggestionProvider<S> customSuggestions) {
        super(name, type.mappedArgument().nativeType(), command, requirement, contextRequirement, redirect, modifier, forks, Objects.requireNonNullElse(customSuggestions, type::listSuggestions));
        this.customType = type;
    }
    public CustomArgumentCommandNode(String name, CustomArgumentType<T, N> type, Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks, SuggestionProvider<S> customSuggestions) {
        super(name, type.mappedArgument().nativeType(), command, requirement, redirect, modifier, forks, Objects.requireNonNullElse(customSuggestions, type::listSuggestions));
        this.customType = type;
    }

    @Override
    public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final T result = customType.parse(reader, contextBuilder.getSource());
        final ParsedArgument<S, T> parsed = new ParsedArgument<>(start, reader.getCursor(), result);

        contextBuilder.withArgument(getName(), parsed);
        contextBuilder.withNode(this, parsed.getRange());
    }
    @Override
    public boolean isValidInput(final String input) {
        try {
            final StringReader reader = new StringReader(input);
            var v = customType.parse(reader);
            return !reader.canRead() || reader.peek() == ' ';
        } catch (final CommandSyntaxException ignored) {
            return false;
        }
    }
    @Override
    public Collection<String> getExamples() {
        return customType.getExamples();
    }

    @Override
    public RequiredArgumentBuilder<S, N> createBuilder() {
        RequiredArgumentBuilder<S, ?> builder = Commands.customArgument(getName(), customType);
        builder.requires(getRequirement());
        builder.forward(getRedirect(), getRedirectModifier(), isFork());
        builder.suggests(getCustomSuggestions());
        if (getCommand() != null)
            builder.executes(getCommand());
        return (RequiredArgumentBuilder<S, N>)builder;
    }
}
