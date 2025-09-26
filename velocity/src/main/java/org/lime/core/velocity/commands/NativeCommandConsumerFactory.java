package org.lime.core.velocity.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Range;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.RepeatableArgumentBuilder;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.velocity.BaseVelocityPlugin;
import org.lime.core.velocity.commands.brigadier.Commands;
import org.lime.core.velocity.commands.brigadier.CustomArgumentType;

import java.util.List;
import java.util.function.Predicate;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSource, NativeCommandConsumerFactory.NativeRegister> {
    public static final NativeCommandConsumerFactory INSTANCE = new NativeCommandConsumerFactory();

    public record NativeRegister(
            BaseVelocityPlugin plugin,
            CommandManager manager,
            List<Command<CommandSource>> commands)
            implements NativeCommandConsumer.NativeRegister<CommandSource> {
        @Override
        public Disposable registerSingle(String alias, Action1<LiteralArgumentBuilder<CommandSource>> configure) {
            var root = Commands.literal(alias);
            configure.invoke(root);
            var meta = manager.metaBuilder(alias)
                    .plugin(plugin)
                    .build();
            manager.register(meta, new BrigadierCommand(root));
            return () -> manager.unregister(meta);
        }
    }

    @Override
    public Class<NativeRegister> builderClass() {
        return NativeRegister.class;
    }
    @Override
    public Class<CommandSource> senderClass() {
        return CommandSource.class;
    }
    @Override
    public Audience audience(CommandSource commandSource) {
        return commandSource;
    }

    @Override
    public Message tooltip(Component component) {
        return VelocityBrigadierMessage.tooltip(component);
    }
    @Override
    public <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType<>(mappedArgument);
    }

    @Override
    public Predicate<CommandSource> operator() {
        return v -> v.hasPermission("velocity.operator");
    }
    @Override
    public LiteralArgumentBuilder<CommandSource> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }
    @Override
    public <T> RequiredArgumentBuilder<CommandSource, T> argument(String key, ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(key, argumentType);
    }
    @Override
    public <T> RepeatableArgumentBuilder<CommandSource, T> repeatable(String key, ArgumentType<T> argumentType) {
        return RepeatableArgumentBuilder.repeatable(key, argumentType);
    }
    @Override
    public <T> RepeatableArgumentBuilder<CommandSource, T> repeatable(String key, @Range(from = 1, to = RepeatableArgumentBuilder.LIMIT_MAX_COUNT) int maxCount, ArgumentType<T> argumentType) {
        return RepeatableArgumentBuilder.repeatable(key, maxCount, argumentType);
    }
}
