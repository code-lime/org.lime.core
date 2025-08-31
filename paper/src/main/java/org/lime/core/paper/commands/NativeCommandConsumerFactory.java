package org.lime.core.paper.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.NativeCommandConsumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSourceStack, NativeCommandConsumerFactory.NativeRegister> {
    public static final NativeCommandConsumerFactory INSTANCE = new NativeCommandConsumerFactory();

    public record NativeRegister(LifecycleEventManager<@NotNull Plugin> eventManager)
            implements NativeCommandConsumer.NativeRegister<CommandSourceStack> {
        @Override
        public void register(LiteralArgumentBuilder<CommandSourceStack> node, String command, List<String> aliases, @Nullable String description) {
            eventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(node.build(), description, aliases));
        }
    }

    @Override
    public Class<NativeRegister> builderClass() {
        return NativeRegister.class;
    }

    @Override
    public Message tooltip(Component component) {
        return new AdventureComponent(component);
    }
    @Override
    public <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType.Converted<@NotNull T, @NotNull N>() {
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
