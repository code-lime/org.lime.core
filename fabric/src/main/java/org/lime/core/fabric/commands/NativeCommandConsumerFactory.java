package org.lime.core.fabric.commands;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.audience.Audience;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
//#switch PROPERTIES.versionAdventurePlatform
//#caseofregex 6\.\d\.\d
//OF//import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
//OF//import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
//#default
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
//#endswitch
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.*;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.JsonInput;
import org.lime.core.common.api.commands.brigadier.arguments.SnbtJsonArgument;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;

import java.util.List;
import java.util.function.Predicate;

public class NativeCommandConsumerFactory
        implements NativeCommandConsumer.Factory<CommandSourceStack, NativeCommandConsumerFactory.NativeRegister> {
    private final MinecraftServer server;
    //#switch PROPERTIES.versionAdventurePlatform
    //#caseofregex 6\.\d\.\d
    //OF//    private final MinecraftAudiences audiences;
    //OF//    public NativeCommandConsumerFactory(MinecraftServer server) {
    //OF//        this.server = server;
    //OF//        audiences = MinecraftServerAudiences.of(server);
    //OF//    }
    //#default
    private final FabricServerAudiences audiences;
    public NativeCommandConsumerFactory(MinecraftServer server) {
        this.server = server;
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
    public CommandNode<CommandSourceStack> root() {
        return server.getCommands().getDispatcher().getRoot();
    }

    @Override
    public Audience audience(CommandSourceStack stack) {
        return stack;
    }

    @Override
    public Message message(Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex 6\.\d\.\d
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
    public <T> ArgumentType<T> json(JsonInput input, TypeAdapter<T> adapter) {
        ArgumentType<Tag> nativeType = NbtTagArgument.nbtTag();
        return argument(new SnbtJsonArgument<>(nativeType, value -> NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, value), input, adapter));
    }

    @Override
    public Predicate<CommandSourceStack> operator() {
        return v ->
                //#switch PROPERTIES.versionMinecraft
                //#caseof 1.21.11
                //OF//                Commands.LEVEL_GAMEMASTERS.check(v.permissions());
                //#default
                v.hasPermission(Commands.LEVEL_GAMEMASTERS);
                //#endswitch
    }
}
