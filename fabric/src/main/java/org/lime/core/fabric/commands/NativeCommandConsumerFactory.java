package org.lime.core.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;

import java.util.List;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSourceStack, NativeCommandConsumerFactory.NativeRegister> {
    private final FabricServerAudiences audiences;
    public NativeCommandConsumerFactory(MinecraftServer server) {
        audiences = FabricServerAudiences.of(server);
    }

    public record NativeRegister(CommandDispatcher<CommandSourceStack> dispatcher)
            implements NativeCommandConsumer.NativeRegister<CommandSourceStack> {
        @Override
        public void register(LiteralArgumentBuilder<CommandSourceStack> node, String command, List<String> aliases, @Nullable String description) {
            dispatcher.register(node);
        }
    }

    @Override
    public Class<NativeRegister> builderClass() {
        return NativeRegister.class;
    }

    @Override
    public Message tooltip(Component component) {
        return audiences.toNative(component);
    }
    @Override
    public <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType<>(mappedArgument);
    }
}
