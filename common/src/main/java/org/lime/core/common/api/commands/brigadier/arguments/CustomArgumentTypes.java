package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.common.collect.Streams;
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
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.api.BaseBrigadier;
import org.lime.core.common.api.commands.brigadier.exceptions.CommandExceptions;
import org.lime.core.common.api.commands.brigadier.exceptions.Generic2CommandExceptionType;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.execute.Func2;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CustomArgumentTypes {
    public static final Generic2CommandExceptionType<String, Stream<Component>> INCORRECT = CommandExceptions.of((expected, data) -> BaseCoreInstance.global.brigadierTooltip(Component.empty()
            .append(Component.text("Expected "))
            .append(Component.text(String.valueOf(expected)).color(NamedTextColor.AQUA))
            .append(Component.text(". Supported: "))
            .append(Component.join(JoinConfiguration.separator(Component.text(", ")), data
                    .map(v -> Component.empty()
                            .append(v)
                            .color(NamedTextColor.AQUA))
                    .toList()))));
    private static final SimpleCommandExceptionType ERROR_NOT_SOURCE = new SimpleCommandExceptionType(new LiteralMessage("A source is required to run this command here"));

    public static StringArgumentType stringType() {
        return stringType(StringArgumentType.StringType.SINGLE_WORD);
    }
    public static StringArgumentType stringType(StringArgumentType.StringType type) {
        return switch (type) {
            case SINGLE_WORD -> StringArgumentType.word();
            case QUOTABLE_PHRASE -> StringArgumentType.string();
            case GREEDY_PHRASE -> StringArgumentType.greedyString();
        };
    }

    public static <J, T, S>Builder<J, T, S> builder(
            Class<S> sourceClass,
            Iterable<J> values,
            Func1<J, String> serialize,
            Func2<String, Boolean, Optional<J>> deserialize,
            Func1<J, T> convert) {
        return new Builder<>(sourceClass, values, serialize, deserialize, convert);
    }
    public static <J, T, S>Builder<J, T, S> builder(
            Class<S> sourceClass,
            Iterable<J> values,
            Func1<J, String> serialize,
            Func1<String, Optional<J>> deserialize,
            Func1<J, T> convert) {
        return new Builder<>(sourceClass, values, serialize, (v0,v1) -> deserialize.invoke(v0), convert);
    }

    public static <T, S>Builder<Map.Entry<String, T>, T, S> builder(
            Class<S> sourceClass,
            Iterable<? extends Map.Entry<String, T>> values) {
        return builder(sourceClass, values, Map.Entry::getKey, Map.Entry::getValue);
    }
    public static <J, T, S>Builder<J, T, S> builder(
            Class<S> sourceClass,
            Iterable<? extends J> values,
            Func1<J, String> key,
            Func1<J, T> value) {
        return new Builder<>(sourceClass, values, key, (v, ignoreCase) -> {
            for (var kv : values)
                if (ignoreCase ? key.invoke(kv).equalsIgnoreCase(v) : key.invoke(kv).equals(v))
                    return Optional.of(kv);
            return Optional.empty();
        }, value);
    }

    public static <T extends Enum<T>, S>Builder<Map.Entry<String, T>, T, S> builderEnum(
            Class<S> sourceClass,
            Class<T> enumClass) {
        return builder(sourceClass, Stream.of(enumClass.getEnumConstants())
                .map(v -> new AbstractMap.SimpleEntry<>(v.name(), v))
                .toList());
    }
    public static <T extends Enum<T>, S>Builder<T, T, S> builderEnum(
            Class<S> sourceClass,
            Iterable<T> enums) {
        return builder(sourceClass, enums, Enum::name, v -> v);
    }
    public static <S>Builder<String, String, S> builderString(
            Class<S> sourceClass,
            Iterable<String> values) {
        return builder(sourceClass, values, v -> v, v -> v);
    }

    public static class Builder<J, T, S> {
        private final Class<S> sourceClass;
        private final Iterable<? extends J> values;
        private final Func1<J, String> serialize;
        private final Func2<String, Boolean, Optional<J>> deserialize;
        private final Func1<J, T> convert;

        private StringArgumentType.StringType type = StringArgumentType.StringType.SINGLE_WORD;
        private boolean ignoreCase = false;
        private @Nullable Func1<J, @Nullable Component> tooltip = null;
        private @Nullable Func2<J, S, Boolean> filter = null;

        Builder(Class<S> sourceClass, Iterable<? extends J> values, Func1<J, String> serialize, Func2<String, Boolean, Optional<J>> deserialize, Func1<J, T> convert) {
            this.sourceClass = sourceClass;
            this.values = values;
            this.serialize = serialize;
            this.deserialize = deserialize;
            this.convert = convert;
        }

        public Builder<J, T, S> type(StringArgumentType.StringType type) {
            this.type = type;
            return this;
        }
        public Builder<J, T, S> ignoreCase() {
            this.ignoreCase = true;
            return this;
        }
        public Builder<J, T, S> requireCase() {
            this.ignoreCase = false;
            return this;
        }
        public Builder<J, T, S> tooltip(@Nullable Func1<J, @Nullable Component> tooltip) {
            this.tooltip = tooltip;
            return this;
        }
        public Builder<J, T, S> filter(@Nullable Func2<J, S, Boolean> filter) {
            this.filter = filter;
            return this;
        }

        public ArgumentType<@NotNull T> build() {
            StringArgumentType nativeType = stringType(type);
            BaseBrigadier brigadier = BaseCoreInstance.global;
            return brigadier.brigadierArgument(new BaseMappedArgument<@NotNull T, String>() {
                @Override
                public @NotNull ArgumentType<String> nativeType() {
                    return nativeType;
                }
                @Override
                public <Source> @NotNull CompletableFuture<Suggestions> suggestions(@NotNull CommandContext<Source> context, @NotNull SuggestionsBuilder builder) {
                    if (!sourceClass.isInstance(context.getSource()))
                        return builder.buildFuture();
                    S source = sourceClass.cast(context.getSource());
                    values.forEach(value -> {
                        if (filter != null && !filter.invoke(value, source))
                            return;

                        final String rawValue = serialize.invoke(value);

                        final String input;
                        final String checkValue;

                        if (ignoreCase) {
                            input = builder.getRemainingLowerCase();
                            checkValue = rawValue.toLowerCase(Locale.ROOT);
                        } else {
                            input = builder.getRemaining();
                            checkValue = rawValue;
                        }

                        if (checkValue.startsWith(input)) {
                            builder.suggest(rawValue, Optional.ofNullable(tooltip)
                                    .map(v -> v.invoke(value))
                                    .map(brigadier::brigadierTooltip)
                                    .orElse(null));
                        }
                    });
                    return builder.buildFuture();
                }

                private CommandSyntaxException incorrect(String value, Stream<? extends J> values) {
                    return INCORRECT.create(value, values
                            .map(v -> {
                                Component component = Component.text(serialize.invoke(v));
                                if (tooltip != null) {
                                    Component tooltipValue = tooltip.invoke(v);
                                    if (tooltipValue != null)
                                        component = component.hoverEvent(HoverEvent.showText(tooltipValue));
                                }
                                return component;
                            }));
                }

                @Override
                public <Source> @NotNull T convert(@NotNull String value, @NotNull Source source) throws CommandSyntaxException {
                    if (filter == null)
                        return convert(value);
                    if (!sourceClass.isInstance(source))
                        throw ERROR_NOT_SOURCE.create();
                    S src = sourceClass.cast(source);
                    return deserialize.invoke(value, ignoreCase)
                            .filter(v -> filter.invoke(v, src))
                            .map(convert)
                            .orElseThrow(() -> incorrect(value, Streams.stream(values)
                                    .filter(v -> filter.invoke(v, src))));
                }
                @Override
                public @NotNull T convert(@NotNull String value) throws CommandSyntaxException {
                    if (filter != null)
                        throw ERROR_NOT_SOURCE.create();
                    return deserialize.invoke(value, ignoreCase)
                            .map(convert)
                            .orElseThrow(() -> incorrect(value, Streams.stream(values)));
                }
            });
        }
    }
}
