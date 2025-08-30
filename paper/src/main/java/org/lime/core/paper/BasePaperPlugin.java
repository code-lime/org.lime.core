package org.lime.core.paper;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitSettings;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.api.commands.LiteCommandConsumer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.utils.Disposable;
import org.lime.core.paper.commands.LiteCommandConsumerFactory;
import org.lime.core.paper.commands.NativeCommandConsumerFactory;

import java.io.File;

public abstract class BasePaperPlugin
        extends JavaPlugin {
    protected final BasePaperInstance<?> instance;
    protected BasePaperPlugin() {
        this.instance = instance();
    }

    protected static class Instance
            extends BasePaperInstance<Instance> {
        private LiteCommandConsumerFactory.Register liteCommandsRegister;

        public Instance(BasePaperPlugin plugin) {
            super(plugin);
        }

        @Override
        public void enable() {
            LiteCommandsBuilder<CommandSender, LiteBukkitSettings, ?> liteCommandsBuilder = LiteBukkitFactory.builder(plugin);
            liteCommandsRegister = new LiteCommandConsumerFactory.Register(liteCommandsBuilder);
            super.enable();
            var liteCommands = liteCommandsBuilder.build();
            liteCommands.register();
            compositeDisposable.add(liteCommands::unregister);
        }

        @Override
        protected BasePaperInstanceModule<Instance> createModule() {
            return plugin.module(this);
        }

        @Override
        protected ClassLoader loader() {
            return plugin.getClassLoader();
        }
        @Override
        protected File pluginFile() {
            return plugin.getFile();
        }

        @Override
        protected Disposable registerCommand(CommandConsumer<?> command) {
            if (command instanceof LiteCommandConsumer<?,?,?> liteCommand) {
                liteCommand.applyCast(liteCommandsRegister);
                return Disposable.empty();
            } else if (command instanceof NativeCommandConsumer<?,?> nativeCommand) {
                plugin.getLifecycleManager()
                        .registerEventHandler(LifecycleEvents.COMMANDS, commands -> nativeCommand.applyCast(new NativeCommandConsumerFactory.Register(commands.registrar())));
                return Disposable.empty();
            } else
                throw new IllegalArgumentException("Not supported " + command.getClass() + " command supplier");
        }
    }

    protected abstract BasePaperInstanceModule<Instance> module(Instance instance);
    protected Instance instance() {
        return new Instance(this);
    }

    @Override
    public void onEnable() {
        instance.enable();
    }
    @Override
    public void onDisable() {
        instance.disable();
    }
}
