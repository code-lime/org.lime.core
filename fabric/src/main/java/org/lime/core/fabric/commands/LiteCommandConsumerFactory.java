package org.lime.core.fabric.commands;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.fabric.LiteFabricSettings;
import net.minecraft.commands.CommandSourceStack;
import org.lime.core.common.api.commands.LiteCommandConsumer;

public class LiteCommandConsumerFactory
        implements LiteCommandConsumer.Factory<CommandSourceStack, LiteFabricSettings, LiteCommandConsumerFactory.Register> {
    public static final LiteCommandConsumerFactory INSTANCE = new LiteCommandConsumerFactory();

    public record Register(LiteCommandsBuilder<CommandSourceStack, LiteFabricSettings, ?> builder)
            implements LiteCommandConsumer.Register<CommandSourceStack, LiteFabricSettings> { }

    @Override
    public Class<Register> builderClass() {
        return Register.class;
    }
}
