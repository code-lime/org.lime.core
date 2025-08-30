package org.lime.core.paper;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.lime.core.common.Artifact;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class BasePaperInstance<Instance extends BasePaperInstance<Instance>>
        extends BaseInstance<Instance> {
    public final BasePaperPlugin plugin;
    private final boolean isCore;
    private final Logger logger;

    public BasePaperInstance(BasePaperPlugin plugin) {
        this.plugin = plugin;
        this.logger = LoggerFactory.getLogger(plugin.getClass());
        this.isCore = plugin.getClass() == CorePaperPlugin.class;
    }

    @Override
    protected void enableService(Service service) {
        if (service instanceof Listener listener)
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        super.enableService(service);
    }
    @Override
    protected void disableService(Service service) {
        if (service instanceof Listener listener)
            HandlerList.unregisterAll(listener);
        super.disableService(service);
    }

    @Override
    protected abstract BasePaperInstanceModule<Instance> createModule();
    protected abstract File pluginFile();

    @Override
    protected Logger logger() {
        return logger;
    }
    @Override
    protected File dataFolder() {
        return plugin.getDataFolder();
    }
    @Override
    public String name() {
        return this.plugin.getName();
    }
    @Override
    public Artifact artifact() {
        return Artifact.PAPER;
    }
    @Override
    protected final boolean isCore() {
        return isCore;
    }
    @Override
    protected Stream<Path> jars() {
        return Stream.of(pluginFile().toPath());
    }
}
