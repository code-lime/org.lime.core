package org.lime.core.common.services;

import com.google.inject.Inject;
import dev.rollczi.litecommands.argument.ArgumentKey;
import dev.rollczi.litecommands.argument.SimpleArgument;
import dev.rollczi.litecommands.argument.resolver.collector.VarargsProfile;
import dev.rollczi.litecommands.programmatic.LiteCommand;
import dev.rollczi.litecommands.reflect.type.TypeToken;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.lime.core.common.Artifact;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.RequireCommand;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.api.commands.LiteCommandConsumer;

import java.util.List;

public class UpdateConfigService
        implements Service {
    @Inject BaseInstance<?> instance;
    @Inject LiteCommandConsumer.Factory<?,?,?> commandsFactory;

    @RequireCommand
    CommandConsumer<?> command() {
        SimpleArgument<String[]> configArgument = new SimpleArgument<>("configs", TypeToken.of(String[].class));
        configArgument.addProfile(new VarargsProfile(TypeToken.of(String.class), " "));
        configArgument.setKey(ArgumentKey.of("update.config#configs"));

        Artifact artifact = instance.artifact();
        String suffix = artifact.proxy ? "." + artifact.key : "";

        return commandsFactory.ofDynamic(new LiteCommand<>("update.config" + suffix)
                .permissions("update.config" + suffix)
                .literal(instance.name())
                .argument(configArgument)
                .execute(ctx -> {
                    String[] configs = ctx.argument(configArgument.getName(), configArgument.getType().getRawType());
                    instance.module.updateConfigs(List.of(configs));
                }))
                .with(v -> v.argumentSuggester(
                        String.class, configArgument.getKey(),
                        (invocation, argument, context) -> SuggestionResult.of(instance.module.configKeys())));
    }
}
