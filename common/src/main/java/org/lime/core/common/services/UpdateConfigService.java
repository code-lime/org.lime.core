package org.lime.core.common.services;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import net.kyori.adventure.text.Component;
import org.lime.core.common.Artifact;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.RequireCommand;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.RepeatableArgumentBuilder;

import java.util.List;

public class UpdateConfigService
        implements Service {
    @Inject BaseInstance<?> instance;
    @Inject NativeCommandConsumer.Factory<?, ?> commandsFactory;
    @Inject CustomArgumentUtility argumentsFactory;

    private <Sender>CommandConsumer<?> command(
            NativeCommandConsumer.Factory<Sender, ?> commandsFactory) {
        Artifact artifact = instance.artifact();
        String suffix = artifact.proxy ? "." + artifact.key : "";

        return commandsFactory.of("update.config" + suffix, j -> j
                .requires(commandsFactory.operator())
                .then(commandsFactory.literal(instance.id())
                        .requires(ctx -> !instance.module.configKeys().isEmpty())
                        .then(commandsFactory.repeatable("config", argumentsFactory.builderString(commandsFactory.senderClass(), instance.module.configKeys()).build())
                                .executes(ctx -> {
                                    List<String> configs = RepeatableArgumentBuilder.readRepeatable(ctx, "config", String.class).toList();
                                    int updateCount = instance.module.updateConfigs(configs);
                                    commandsFactory.audience(ctx.getSource())
                                            .sendMessage(Component.text("Configs '"+String.join("', '", configs)+"' update "+updateCount+" access"));
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }
    @RequireCommand
    CommandConsumer<?> command() {
        return command(commandsFactory);
    }
}
