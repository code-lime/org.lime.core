package org.lime.core.common.api.commands;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.LiteCommandsProvider;
import dev.rollczi.litecommands.argument.ArgumentKey;
import dev.rollczi.litecommands.argument.parser.Parser;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolverBase;
import dev.rollczi.litecommands.argument.suggester.Suggester;
import dev.rollczi.litecommands.platform.PlatformSettings;
import dev.rollczi.litecommands.programmatic.LiteCommand;

public interface LiteCommandConsumer<Sender, Settings extends PlatformSettings, Builder extends LiteCommandConsumer.Register<Sender, Settings>>
        extends CommandConsumer<Builder> {
    interface Register<Sender, Settings extends PlatformSettings> {
        LiteCommandsBuilder<Sender, Settings, ?> builder();

        default void command(Object command) {
            builder().commands(command);
        }

        default <T>void argument(Class<T> argumentClass, ArgumentResolverBase<Sender, T> resolver) {
            argument(argumentClass, ArgumentKey.of(), resolver);
        }
        default <T>void argument(Class<T> argumentClass, ArgumentKey key, ArgumentResolverBase<Sender, T> resolver) {
            builder().argument(argumentClass, key, resolver);
        }

        default <T>void argumentSuggester(Class<T> argumentClass, Suggester<Sender, T> suggester) {
            argumentSuggester(argumentClass, ArgumentKey.of(), suggester);
        }
        default <T>void argumentSuggester(Class<T> argumentClass, ArgumentKey key, Suggester<Sender, T> suggester) {
            builder().argumentSuggester(argumentClass, key, suggester);
        }

        default <T>void argumentParser(Class<T> argumentClass, Parser<Sender, T> parser) {
            argumentParser(argumentClass, ArgumentKey.of(), parser);
        }
        default <T>void argumentParser(Class<T> argumentClass, ArgumentKey key, Parser<Sender, T> parser) {
            builder().argumentParser(argumentClass, key, parser);
        }
    }
    interface Factory<Sender, Settings extends PlatformSettings, Builder extends Register<Sender, Settings>> {
        Class<Builder> builderClass();

        default LiteCommandConsumer<Sender, Settings, Builder> ofDynamic(
                Object command) {
            return new LiteCommandConsumer<>() {
                @Override
                public void apply(Builder builder) {
                    builder.command(command);
                }
                @Override
                public Class<Builder> builderClass() {
                    return Factory.this.builderClass();
                }
            };
        }
        default LiteCommandConsumer<Sender, Settings, Builder> of(
                LiteCommand<Sender> command) {
            return ofDynamic(command);
        }
        default LiteCommandConsumer<Sender, Settings, Builder> of(
                LiteCommandsProvider<Sender> commandProvider) {
            return ofDynamic(commandProvider);
        }
        default <T> LiteCommandConsumer<Sender, Settings, Builder> of(
                Class<T> commandClass) {
            return ofDynamic(commandClass);
        }

        default <T> LiteCommandConsumer<Sender, Settings, Builder> ofArgument(
                Class<T> argumentClass,
                ArgumentResolverBase<Sender, T> resolver) {
            return ofArgument(argumentClass, ArgumentKey.of(), resolver);
        }
        default <T> LiteCommandConsumer<Sender, Settings, Builder> ofArgument(
                Class<T> argumentClass,
                ArgumentKey key,
                ArgumentResolverBase<Sender, T> resolver) {
            return new LiteCommandConsumer<>() {
                @Override
                public void apply(Builder builder) {
                    builder.argument(argumentClass, key, resolver);
                }
                @Override
                public Class<Builder> builderClass() {
                    return Factory.this.builderClass();
                }
            };
        }
    }
}
