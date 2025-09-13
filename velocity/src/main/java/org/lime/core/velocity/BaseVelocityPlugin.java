package org.lime.core.velocity;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.Artifact;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.json.builder.Json;
import org.lime.core.common.utils.system.execute.ActionEx2;
import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple1;
import org.lime.core.velocity.commands.NativeCommandConsumerFactory;
import org.lime.core.velocity.libby.VelocityDependencyLoader;
import org.lime.core.velocity.tasks.VelocityScheduleTaskService;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

public abstract class BaseVelocityPlugin
        extends BaseInstance<BaseVelocityPlugin> {
    protected static VelocityDependencyLoader globalDependencyLoader;

    private final boolean isCore;
    public final ProxyServer server;
    protected final Logger logger;
    protected final File dataFolder;
    protected final ScheduleTaskService taskService;
    private Iterable<CommandConsumer.BaseRegister> commandRegisters;

    private PluginDescription description;
    private VelocityDependencyLoader dependencyLoader;

    protected BaseVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.isCore = this.getClass() == CoreVelocityPlugin.class;

        this.server = server;
        this.logger = logger;
        this.dataFolder = dataDirectory.toFile();
        this.taskService = new VelocityScheduleTaskService(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        description = server.getPluginManager()
                .fromInstance(this)
                .map(PluginContainer::getDescription)
                .orElseThrow(() -> new IllegalArgumentException("Description of plugin '"+this.getClass()+"' not found"));
        enable();
    }
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        disable();
    }

    protected void loadDependencies(JsonObject velocityDependency) {
        if (velocityDependency.has("maven"))
            velocityDependency.getAsJsonObject("maven")
                    .entrySet()
                    .forEach(kv -> dependencyLoader.loadRepository(kv.getKey(), kv.getValue().getAsString()));
        if (velocityDependency.has("libraries"))
            velocityDependency.getAsJsonArray("libraries")
                    .forEach(v -> dependencyLoader.loadDependency(v.getAsString()));
    }
    protected void analyzePluginJar(ActionEx2<JarEntry, InputStream> analyzer) {
        description
                .getSource()
                .ifPresent(path -> {
                    try (JarInputStream zis = new JarInputStream(new ByteArrayInputStream(Files.readAllBytes(path)))) {
                        JarEntry jarEntry;
                        while ((jarEntry = zis.getNextJarEntry()) != null) {
                            if (!jarEntry.isDirectory())
                                analyzer.invoke(jarEntry, zis);
                            zis.closeEntry();
                        }
                    } catch (Throwable e) {
                        throw new IllegalArgumentException(e);
                    }
                });
    }

    @Override
    public void enable() {
        dependencyLoader = Objects.requireNonNull(globalDependencyLoader);

        Tuple1<JsonObject> velocityDependency = Tuple.of(null);
        analyzePluginJar((entry, stream) -> {
            String entryName = entry.getName();
            if (!entryName.equals("velocity-dependency.json"))
                return;
            velocityDependency.val0 = Json.parse(Charsets.UTF_8
                            .decode(ByteBuffer.wrap(stream.readAllBytes()))
                            .toString()
                            .replace("\r", ""))
                    .getAsJsonObject();
        });

        if (velocityDependency.val0 != null)
            loadDependencies(velocityDependency.val0);

        commandRegisters = List.of(
                new NativeCommandConsumerFactory.NativeRegister(this, server.getCommandManager(), new ArrayList<>()));
        super.enable();
        commandRegisters.forEach(v -> compositeDisposable.add(v.apply()));
    }

    @Override
    protected Iterable<CommandConsumer.BaseRegister> commandRegisters() {
        return commandRegisters;
    }

    @Override
    protected void enableService(Service service) {
        server.getEventManager().register(this, service);
        super.enableService(service);
    }
    @Override
    protected void disableService(Service service) {
        super.disableService(service);
        server.getEventManager().unregisterListener(this, service);
    }

    @Override
    protected abstract BaseVelocityInstanceModule createModule();

    @Override
    protected Logger logger() {
        return logger;
    }
    @Override
    protected ClassLoader loader() {
        return getClass().getClassLoader();
    }
    @Override
    protected File dataFolder() {
        return dataFolder;
    }
    @Override
    public String id() {
        return description.getId();
    }
    @Override
    public String name() {
        return description.getName().orElseGet(description::getId);
    }
    @Override
    public Artifact artifact() {
        return Artifact.VELOCITY;
    }
    @Override
    protected final boolean isCore() {
        return isCore;
    }
    @Override
    protected Stream<Path> jars() {
        return description.getSource()
                .stream();
    }
}
