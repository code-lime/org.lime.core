package org.lime.core.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.velocity.LiteVelocitySettings;
import org.lime.core.common.api.commands.LiteCommandConsumer;

public class LiteCommandConsumerFactory
        implements LiteCommandConsumer.Factory<CommandSource, LiteVelocitySettings, LiteCommandConsumerFactory.LiteRegister> {
    public static final LiteCommandConsumerFactory INSTANCE = new LiteCommandConsumerFactory();

    public record LiteRegister(LiteCommandsBuilder<CommandSource, LiteVelocitySettings, ?> builder)
            implements LiteCommandConsumer.LiteRegister<CommandSource, LiteVelocitySettings> { }

    @Override
    public Class<LiteRegister> registerClass() {
        return LiteRegister.class;
    }
}
