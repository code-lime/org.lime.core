package org.lime.core.common.services;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import org.lime.core.common.Artifact;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.RequireCommand;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.slf4j.Logger;

import java.util.List;

public class UpdateConfigService
        implements Service {
    @Inject BaseInstance<?> instance;
    @Inject NativeCommandConsumer.Factory<?, ?> commandsFactory;
    @Inject CustomArgumentUtility argumentsFactory;
    @Inject Logger logger;

    private <Sender>CommandConsumer<?> command(
            NativeCommandConsumer.Factory<Sender, ?> commandsFactory) {
        Artifact artifact = instance.artifact();
        String suffix = artifact.proxy ? "." + artifact.key : "";

        return commandsFactory.of("update.config" + suffix, j -> j
                .requires(commandsFactory.operator())
                .then(commandsFactory.literal(instance.id())
                        .then(commandsFactory.argument("config", argumentsFactory.builderString(commandsFactory.senderClass(), instance.module.configKeys()).build())
                                .executes(ctx -> {
                                    String config = ctx.getArgument("config", String.class);
                                    instance.module.updateConfigs(List.of(config));
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }
    @RequireCommand
    CommandConsumer<?> command() {
        return command(commandsFactory);
    }
}
