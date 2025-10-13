package org.lime.core.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.MinecraftServer;
//#switch PROPERTIES.versionAdventurePlatform
//#caseof 6.3.0;6.6.0
//OF//import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
//OF//import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
//#default
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
//#endswitch
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Range;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.RepeatableArgumentBuilder;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;

import java.util.List;
import java.util.function.Predicate;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSourceStack, NativeCommandConsumerFactory.NativeRegister> {
    //#switch PROPERTIES.versionAdventurePlatform
    //#caseof 6.3.0;6.6.0
    //OF//    private final MinecraftAudiences audiences;
    //OF//    public NativeCommandConsumerFactory(MinecraftServer server) {
    //OF//        audiences = MinecraftServerAudiences.of(server);
    //OF//    }
    //#default
    private final FabricServerAudiences audiences;
    public NativeCommandConsumerFactory(MinecraftServer server) {
        audiences = FabricServerAudiences.of(server);
    }
    //#endswitch

    public record NativeRegister(
            ScheduleTaskService taskService,
            CommandDispatcher<CommandSourceStack> dispatcher,
            List<Command<CommandSourceStack>> commands)
            implements NativeCommandConsumer.NativeRegister<CommandSourceStack> {
        @Override
        public Disposable registerSingle(String alias, Action1<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            taskService.runNextTick(() -> {
                var root = Commands.literal(alias);
                configure.invoke(root);
                dispatcher.register(root);
            }, true);
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
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseof 6.3.0;6.6.0
        //OF//        return audiences.asNative(component);
        //#default
        return audiences.toNative(component);
        //#endswitch
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
    @Override
    public <T> RepeatableArgumentBuilder<CommandSourceStack, T> repeatable(String key, ArgumentType<T> argumentType) {
        return RepeatableArgumentBuilder.repeatable(this, key, argumentType);
    }
    @Override
    public <T> RepeatableArgumentBuilder<CommandSourceStack, T> repeatable(String key, @Range(from = 1, to = RepeatableArgumentBuilder.LIMIT_MAX_COUNT) int maxCount, ArgumentType<T> argumentType) {
        return RepeatableArgumentBuilder.repeatable(this, key, maxCount, argumentType);
    }
}
