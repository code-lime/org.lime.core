package org.lime.core.paper.commands;

import com.google.gson.*;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.serialization.JsonOps;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.JsonInput;
import org.lime.core.common.api.commands.brigadier.arguments.SnbtJsonArgument;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.reflection.*;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSourceStack, NativeCommandConsumerFactory.NativeRegister> {
    public static final NativeCommandConsumerFactory INSTANCE = new NativeCommandConsumerFactory();
    private static final Object VANILLA_ARGUMENT_PROVIDER = ReflectionMethod
            .of(Reflection.findClass("io.papermc.paper.command.brigadier.argument.VanillaArgumentProvider"), "provider")
            .lambda(Func0.class)
            .invoke();
    @SuppressWarnings("unchecked")
    private static final Func2<Object, ArgumentType<?>, ArgumentType<?>> WRAP_ARGUMENT = ReflectionMethod
            .of(VANILLA_ARGUMENT_PROVIDER.getClass(), "wrap", ArgumentType.class)
            .lambda(Func2.class);

    public record NativeRegister(
            LifecycleEventManager<@NotNull Plugin> eventManager,
            List<Command<CommandSourceStack>> commands)
            implements NativeCommandConsumer.NativeRegister<CommandSourceStack> {
        @Override
        public Disposable registerSingle(String alias, Action1<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            eventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                var root = Commands.literal(alias);
                configure.invoke(root);
                commands.registrar().register(root.build());
            });
            return Disposable.empty();
        }
    }

    @Override
    public Class<NativeRegister> builderClass() {
        return NativeRegister.class;
    }
    @Override
    public Class<CommandSourceStack> senderClass() {
        return CommandSourceStack.class;
    }

    @Override
    public CommandNode<CommandSourceStack> root() {
        return PaperCommands.INSTANCE.getDispatcher().getRoot();
    }

    @Override
    public Audience audience(CommandSourceStack commandSourceStack) {
        return commandSourceStack.getSender();
    }

    @Override
    public Message message(Component component) {
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
    @Override
    public <T> ArgumentType<T> json(JsonInput input, TypeAdapter<T> adapter) {
        return argument(new SnbtJsonArgument<>(wrap(NbtTagArgument.nbtTag()), value -> NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, value), input, adapter));
    }
    @SuppressWarnings("unchecked")
    public <T> ArgumentType<T> wrap(ArgumentType<T> vanillaType) {
        return (ArgumentType<T>) WRAP_ARGUMENT.invoke(VANILLA_ARGUMENT_PROVIDER, vanillaType);
    }

    @Override
    public Predicate<CommandSourceStack> operator() {
        return v -> v.getSender().isOp();
    }
}
