package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.exceptions.Generic2CommandExceptionType;
import org.lime.core.common.utils.system.execute.Func1;
import org.lime.core.common.utils.system.execute.Func2;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CustomArgumentBuilder<J, T, Source> {
    private final NativeCommandConsumer.Factory<?, ?> factory;
    private final Generic2CommandExceptionType<String, Stream<Component>> incorrectException;
    private final SimpleCommandExceptionType notSourceException;

    private final Class<Source> sourceClass;
    private final Iterable<? extends J> values;
    private final Func1<J, String> serialize;
    private final Func2<String, Boolean, Optional<J>> deserialize;
    private final Func1<J, T> convert;

    private StringArgumentType.StringType type = StringArgumentType.StringType.SINGLE_WORD;
    private boolean ignoreCase = false;
    private @Nullable Func1<J, @Nullable Component> tooltip = null;
    private @Nullable Func2<J, Source, Boolean> filter = null;

    public CustomArgumentBuilder(
            NativeCommandConsumer.Factory<?, ?> factory,
            Generic2CommandExceptionType<String, Stream<Component>> incorrectException,
            SimpleCommandExceptionType notSourceException,
            Class<Source> sourceClass,
            Iterable<? extends J> values,
            Func1<J, String> serialize,
            Func2<String, Boolean, Optional<J>> deserialize,
            Func1<J, T> convert) {
        this.factory = factory;
        this.incorrectException = incorrectException;
        this.notSourceException = notSourceException;

        this.sourceClass = sourceClass;
        this.values = values;
        this.serialize = serialize;
        this.deserialize = deserialize;
        this.convert = convert;
    }

    public CustomArgumentBuilder<J, T, Source> type(StringArgumentType.StringType type) {
        this.type = type;
        return this;
    }

    public CustomArgumentBuilder<J, T, Source> ignoreCase() {
        this.ignoreCase = true;
        return this;
    }

    public CustomArgumentBuilder<J, T, Source> requireCase() {
        this.ignoreCase = false;
        return this;
    }

    public CustomArgumentBuilder<J, T, Source> tooltip(@Nullable Func1<J, @Nullable Component> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public CustomArgumentBuilder<J, T, Source> filter(@Nullable Func2<J, Source, Boolean> filter) {
        this.filter = filter;
        return this;
    }

    public ArgumentType<@NotNull T> build() {
        StringArgumentType nativeType = switch (type) {
            case SINGLE_WORD -> StringArgumentType.word();
            case QUOTABLE_PHRASE -> StringArgumentType.string();
            case GREEDY_PHRASE -> StringArgumentType.greedyString();
        };
        return factory.argument(new BaseMappedArgument<@NotNull T, String>() {
            @Override
            public @NotNull ArgumentType<String> nativeType() {
                return nativeType;
            }

            private SuggestionsBuilder withError(SuggestionsBuilder builder, String error) {
                return builder.suggest("§cERROR (hover for details)§r", factory.tooltip(Component.text(error)));
            }

            @Override
            public <ContextSource> @NotNull CompletableFuture<Suggestions> suggestions(@NotNull CommandContext<ContextSource> context, @NotNull SuggestionsBuilder builder) {
                if (!sourceClass.isInstance(context.getSource())) {
                    return withError(builder, "Source '"+context.getSource().getClass()+"' is not " + sourceClass).buildFuture();
                }
                Source source = sourceClass.cast(context.getSource());
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
                                .map(factory::tooltip)
                                .orElse(null));
                    }
                });
                return builder.buildFuture();
            }

            private CommandSyntaxException incorrect(String value, Stream<? extends J> values) {
                return incorrectException.create(value, values
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
            public <ContextSource> @NotNull T convert(@NotNull String value, @NotNull ContextSource source) throws CommandSyntaxException {
                if (filter == null)
                    return convert(value);
                if (!sourceClass.isInstance(source))
                    throw notSourceException.create();
                Source src = sourceClass.cast(source);
                return deserialize.invoke(value, ignoreCase)
                        .filter(v -> filter.invoke(v, src))
                        .map(convert)
                        .orElseThrow(() -> incorrect(value, Streams.stream(values)
                                .filter(v -> filter.invoke(v, src))));
            }

            @Override
            public @NotNull T convert(@NotNull String value) throws CommandSyntaxException {
                if (filter != null)
                    throw notSourceException.create();
                return deserialize.invoke(value, ignoreCase)
                        .map(convert)
                        .orElseThrow(() -> incorrect(value, Streams.stream(values)));
            }
        });
    }
}
