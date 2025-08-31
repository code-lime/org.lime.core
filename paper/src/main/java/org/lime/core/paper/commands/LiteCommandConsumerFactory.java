package org.lime.core.paper.commands;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.bukkit.LiteBukkitSettings;
import org.bukkit.command.CommandSender;
import org.lime.core.common.api.commands.LiteCommandConsumer;

public class LiteCommandConsumerFactory
        implements LiteCommandConsumer.Factory<CommandSender, LiteBukkitSettings, LiteCommandConsumerFactory.LiteRegister> {
    public static final LiteCommandConsumerFactory INSTANCE = new LiteCommandConsumerFactory();

    public record LiteRegister(LiteCommandsBuilder<CommandSender, LiteBukkitSettings, ?> builder)
            implements LiteCommandConsumer.LiteRegister<CommandSender, LiteBukkitSettings> { }

    @Override
    public Class<LiteRegister> registerClass() {
        return LiteRegister.class;
    }
}
