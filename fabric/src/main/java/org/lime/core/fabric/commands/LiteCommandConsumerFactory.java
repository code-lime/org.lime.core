package org.lime.core.fabric.commands;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.fabric.LiteFabricSettings;
import net.minecraft.commands.CommandSourceStack;
import org.lime.core.common.api.commands.LiteCommandConsumer;
import org.lime.core.common.utils.Disposable;

public class LiteCommandConsumerFactory
        implements LiteCommandConsumer.Factory<CommandSourceStack, LiteFabricSettings, LiteCommandConsumerFactory.LiteRegister> {
    public static final LiteCommandConsumerFactory INSTANCE = new LiteCommandConsumerFactory();

    public record LiteRegister(LiteCommandsBuilder<CommandSourceStack, LiteFabricSettings, ?> builder)
            implements LiteCommandConsumer.LiteRegister<CommandSourceStack, LiteFabricSettings> {
        @Override
        public Disposable apply() {
            var liteCommands = builder.build();
            liteCommands.register();
            return liteCommands::unregister;
        }
    }

    @Override
    public Class<LiteRegister> registerClass() {
        return LiteRegister.class;
    }
}
