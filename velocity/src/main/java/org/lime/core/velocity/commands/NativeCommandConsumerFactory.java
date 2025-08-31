package org.lime.core.velocity.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.velocity.BaseVelocityPlugin;
import org.lime.core.velocity.commands.brigadier.CustomArgumentType;

import java.util.List;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSource, NativeCommandConsumerFactory.NativeRegister> {
    public static final NativeCommandConsumerFactory INSTANCE = new NativeCommandConsumerFactory();

    public record NativeRegister(
            BaseVelocityPlugin plugin,
            CommandManager commands)
            implements NativeCommandConsumer.NativeRegister<CommandSource> {
        @Override
        public void register(LiteralArgumentBuilder<CommandSource> node, String command, List<String> aliases, @Nullable String description) {
            var meta = commands.metaBuilder(command)
                    .plugin(plugin)
                    .aliases(aliases.toArray(new String[0]))
                    .build();
            commands.register(meta, new BrigadierCommand(node));
        }
    }

    @Override
    public Class<NativeRegister> builderClass() {
        return NativeRegister.class;
    }

    @Override
    public Message tooltip(Component component) {
        return VelocityBrigadierMessage.tooltip(component);
    }
    @Override
    public <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType<>(mappedArgument);
    }
}
