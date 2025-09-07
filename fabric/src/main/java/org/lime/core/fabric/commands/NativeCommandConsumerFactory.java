package org.lime.core.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.system.execute.Action1;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;

import java.util.List;
import java.util.function.Predicate;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSourceStack, NativeCommandConsumerFactory.NativeRegister> {
    private final FabricServerAudiences audiences;
    public NativeCommandConsumerFactory(MinecraftServer server) {
        audiences = FabricServerAudiences.of(server);
    }

    public record NativeRegister(
            CommandDispatcher<CommandSourceStack> dispatcher,
            List<Command<CommandSourceStack>> commands)
            implements NativeCommandConsumer.NativeRegister<CommandSourceStack> {
        @Override
        public Disposable registerSingle(String alias, Action1<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            var root = Commands.literal(alias);
            configure.invoke(root);
            dispatcher.register(root);
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
    public Audience audience(CommandSourceStack stack) {
        return stack;
    }

    @Override
    public Message tooltip(Component component) {
        return audiences.toNative(component);
    }
    @Override
    public <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType<>(mappedArgument);
    }

    @Override
    public Predicate<CommandSourceStack> operator() {
        return v -> v.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> literal(String literal) {
        return Commands.literal(literal);
    }
    @Override
    public <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String key, ArgumentType<T> argumentType) {
        return Commands.argument(key, argumentType);
    }
}
