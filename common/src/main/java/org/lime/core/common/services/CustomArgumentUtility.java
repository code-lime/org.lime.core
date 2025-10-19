package org.lime.core.common.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.CustomArgumentBuilder;
import org.lime.core.common.api.commands.brigadier.exceptions.CommandExceptions;
import org.lime.core.common.api.commands.brigadier.exceptions.Generic2CommandExceptionType;
import org.lime.core.common.utils.DurationUtils;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public class CustomArgumentUtility {
    @Inject NativeCommandConsumer.Factory<?, ?> factory;

    private final Generic2CommandExceptionType<String, Stream<Component>> INCORRECT = CommandExceptions.of((expected, data) -> factory.tooltip(Component.empty()
            .append(Component.text("Expected "))
            .append(Component.text(String.valueOf(expected)).color(NamedTextColor.AQUA))
            .append(Component.text(". Supported: "))
            .append(Component.join(JoinConfiguration.separator(Component.text(", ")), data
                    .map(v -> Component.empty()
                            .append(v)
                            .color(NamedTextColor.AQUA))
                    .toList()))));
    private final SimpleCommandExceptionType ERROR_NOT_SOURCE = new SimpleCommandExceptionType(new LiteralMessage("A source is required to run this command here"));

    public <J, T, Source> CustomArgumentBuilder<J, T, Source> builder(
            Class<Source> sourceClass,
            Iterable<J> values,
            Func1<J, String> serialize,
            Func2<String, Boolean, Optional<J>> deserialize,
            Func1<J, T> convert) {
        return new CustomArgumentBuilder<>(factory, INCORRECT, ERROR_NOT_SOURCE, sourceClass, values, serialize, deserialize, convert);
    }
    public <J, T, Source> CustomArgumentBuilder<J, T, Source> builder(
            Class<Source> sourceClass,
            Iterable<J> values,
            Func1<J, String> serialize,
            Func1<String, Optional<J>> deserialize,
            Func1<J, T> convert) {
        return new CustomArgumentBuilder<>(factory, INCORRECT, ERROR_NOT_SOURCE, sourceClass, values, serialize, (v0, v1) -> deserialize.invoke(v0), convert);
    }

    public <T, Source> CustomArgumentBuilder<Map.Entry<String, T>, T, Source> builder(
            Class<Source> sourceClass,
            Iterable<? extends Map.Entry<String, T>> values) {
        return builder(sourceClass, values, Map.Entry::getKey, Map.Entry::getValue);
    }
    public <J, T, Source> CustomArgumentBuilder<J, T, Source> builder(
            Class<Source> sourceClass,
            Iterable<? extends J> values,
            Func1<J, String> key,
            Func1<J, T> value) {
        return new CustomArgumentBuilder<>(factory, INCORRECT, ERROR_NOT_SOURCE, sourceClass, values, key, (v, ignoreCase) -> {
            for (var kv : values)
                if (ignoreCase ? key.invoke(kv).equalsIgnoreCase(v) : key.invoke(kv).equals(v))
                    return Optional.of(kv);
            return Optional.empty();
        }, value);
    }

    public <T extends Enum<T>, Source> CustomArgumentBuilder<Map.Entry<String, T>, T, Source> builderEnum(
            Class<Source> sourceClass,
            Class<T> enumClass) {
        return builder(sourceClass, Stream.of(enumClass.getEnumConstants())
                .map(v -> new AbstractMap.SimpleEntry<>(v.name(), v))
                .toList());
    }
    public <T extends Enum<T>, Source> CustomArgumentBuilder<T, T, Source> builderEnum(
            Class<Source> sourceClass,
            Iterable<T> enums) {
        return builder(sourceClass, enums, Enum::name, v -> v);
    }
    public <Source> CustomArgumentBuilder<String, String, Source> builderString(
            Class<Source> sourceClass,
            Iterable<String> values) {
        return builder(sourceClass, values, v -> v, v -> v);
    }

    public ArgumentType<Duration> duration() {
        return factory.argument(new BaseMappedArgument<@NotNull Duration, String>() {
            @Override
            public ArgumentType<String> nativeType() {
                return StringArgumentType.word();
            }
            @Override
            public @NotNull Duration convert(String value) throws CommandSyntaxException {
                try {
                    return DurationUtils.read(value);
                } catch (IllegalStateException state) {
                    throw new SimpleCommandExceptionType(new LiteralMessage(state.getMessage())).create();
                }
            }
            @Override
            public <S> CompletableFuture<Suggestions> suggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                DurationUtils.readVariants(builder.getRemaining())
                        .forEach(builder::suggest);
                return builder.buildFuture();
            }
        });
    }
}
