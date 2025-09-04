package org.lime.core.paper;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.paper.commands.LiteCommandConsumerFactory;
import org.lime.core.paper.commands.NativeCommandConsumerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class BasePaperPlugin
        extends JavaPlugin {
    protected final BasePaperInstance<?> instance;
    protected BasePaperPlugin() {
        this.instance = instance();
    }

    protected static class Instance
            extends BasePaperInstance<Instance> {
        private Iterable<CommandConsumer.BaseRegister> commandRegisters;

        public Instance(BasePaperPlugin plugin) {
            super(plugin);
        }

        @Override
        public void enable() {
            LiteCommandsBuilder<CommandSender, LiteBukkitSettings, ?> liteCommandsBuilder = LiteBukkitFactory.builder(plugin);
            commandRegisters = List.of(
                    new LiteCommandConsumerFactory.LiteRegister(liteCommandsBuilder),
                    new NativeCommandConsumerFactory.NativeRegister(plugin.getLifecycleManager(), new ArrayList<>()));
            super.enable();
            commandRegisters.forEach(v -> compositeDisposable.add(v.apply()));
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
        protected Iterable<CommandConsumer.BaseRegister> commandRegisters() {
            return commandRegisters;
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
