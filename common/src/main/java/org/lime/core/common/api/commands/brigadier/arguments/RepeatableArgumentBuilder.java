package org.lime.core.common.api.commands.brigadier.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.system.execute.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RepeatableArgumentBuilder<S, T>
        extends ArgumentBuilder<S, RepeatableArgumentBuilder<S, T>> {
    public static final int LIMIT_MAX_COUNT = 10;
    private static final Func1<CommandContext<?>, Map<String, ParsedArgument<?, ?>>> ARGUMENTS_GETTER = ReflectionField.of(CommandContext.class, "arguments").access().getter(Func1.class);

    private final String name;
    private final @Range(from = 1, to = LIMIT_MAX_COUNT) int maxCount;
    private final ArgumentType<T> type;
    private @Nullable SuggestionProvider<S> suggestionsProvider = null;

    private RepeatableArgumentBuilder(
            final String name,
            final @Range(from = 1, to = LIMIT_MAX_COUNT) int maxCount,
            final ArgumentType<T> type) {
        this.name = name;
        this.maxCount = maxCount;
        this.type = type;
    }
    public static <S, T> RepeatableArgumentBuilder<S, T> repeatable(
            final String name,
            final @Range(from = 1, to = LIMIT_MAX_COUNT) int maxCount,
            final ArgumentType<T> type) {
        return new RepeatableArgumentBuilder<>(name, maxCount, type);
    }
    public static <S, T> RepeatableArgumentBuilder<S, T> repeatable(
            final String name,
            final ArgumentType<T> type) {
        return new RepeatableArgumentBuilder<>(name, LIMIT_MAX_COUNT, type);
    }

    private String getIndexedName(int index) {
        return getIndexedName(name, index);
    }
    private static String getIndexedName(String name, int index) {
        return name + "[" + index + "]";
    }

    public static <T>Stream<T> readRepeatable(
            final CommandContext<?> ctx,
            final String name,
            final Class<T> type) {
        return IntStream.rangeClosed(0, LIMIT_MAX_COUNT)
                .mapToObj(index -> getIndexedName(name, index))
                .takeWhile(ARGUMENTS_GETTER.apply(ctx)::containsKey)
                .map(indexedName -> ctx.getArgument(indexedName, type));
    }

    public RepeatableArgumentBuilder<S, T> suggests(
            @Nullable SuggestionProvider<S> provider) {
        this.suggestionsProvider = provider;
        return getThis();
    }
    public @Nullable SuggestionProvider<S> getSuggestionsProvider() {
        return suggestionsProvider;
    }

    @Override
    protected RepeatableArgumentBuilder<S, T> getThis() {
        return this;
    }

    public ArgumentType<T> getType() {
        return type;
    }
    public @Range(from = 0, to = LIMIT_MAX_COUNT) int getMaxCount() {
        return maxCount;
    }
    public String getName() {
        return name;
    }

    public RequiredArgumentBuilder<S, T> toNative() {
        @Nullable SuggestionProvider<S> suggestionProvider = getSuggestionsProvider();
        int maxCount = getMaxCount();
        ArgumentType<T> type = getType();

        List<RequiredArgumentBuilder<S, T>> items = new ArrayList<>(maxCount);
        for (int index = 0; index < maxCount; index++) {
            String indexedName = getIndexedName(index);
            RequiredArgumentBuilder<S, T> current = RequiredArgumentBuilder.argument(indexedName, type);
            if (suggestionProvider != null)
                current.suggests(suggestionProvider);
            current.executes(getCommand());
            items.add(current);
        }
        RequiredArgumentBuilder<S, T> builder = items.get(maxCount - 1);
        for (int index = maxCount - 2; index >= 0; index--) {
            RequiredArgumentBuilder<S, T> current = items.get(index);
            current.then(builder);
            builder = current;
        }
        builder.requires(getRequirement());
        return builder;
    }

    @Override
    public CommandNode<S> build() {
        return toNative().build();
    }
}
