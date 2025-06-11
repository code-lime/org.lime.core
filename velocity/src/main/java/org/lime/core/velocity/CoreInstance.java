package org.lime.core.velocity;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.UnsafeMappings;
import org.lime.core.common.agent.Agents;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.common.json.builder.Json;
import org.lime.core.velocity.libby.LibbyVelocityDependencyLoader;
import org.lime.core.velocity.tasks.VelocityScheduleTaskService;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

@Plugin(
        id = BuildConstants.ID,
        name = BuildConstants.NAME,
        version = BuildConstants.VERSION,
        authors = "Lime"
)
public class CoreInstance
        extends BaseCoreInstance<CoreCommand.Register, CoreInstance>
        implements VelocityState, VelocityLogger, VelocityElementAccess, VelocityCommandAccess {
    public static CoreInstance core;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final String dataDirectoryFile;
    private final ScheduleTaskService taskService;

    private VelocityIdentity identity;
    private VelocityDependencyLoader dependencyLoader;

    @Inject
    public CoreInstance(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.dataDirectoryFile = dataDirectory.toAbsolutePath().toString();
        this.taskService = new VelocityScheduleTaskService(this);
    }

    @Override
    public CoreCommand.Register createCommand(String cmd) {
        return CoreCommand.Register.create(cmd);
    }

    @Override
    public ClassLoader classLoader() {
        return this.getClass().getClassLoader();
    }
    @Override
    public ProxyServer server() {
        return server;
    }
    @Override
    public ScheduleTaskService taskService() {
        return taskService;
    }
    public String pluginId() {
        return identity.pluginId();
    }
    @Override
    public String name() {
        return identity.name();
    }
    @Override
    public String configFile() {
        return dataDirectoryFile;
    }
    @Override
    public CoreInstance self() {
        return this;
    }

    @Override
    protected void setBaseCore(CoreInstance core) {
        super.setBaseCore(core);
        CoreInstance.core = core;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        identity = VelocityIdentity.of(server, this)
                .orElseThrow(() -> new IllegalArgumentException("Plugin '"+this.getClass()+"' not found"));
        onLoad();
        enableInstance();
    }
    @Subscribe
    public void onProxyIShutdown(ProxyShutdownEvent event) {
        disableInstance();
    }

    public void onLoad() {}

    private void loadDependencies(JsonObject velocityDependency) {
        if (velocityDependency.has("maven"))
            velocityDependency.getAsJsonObject("maven")
                    .entrySet()
                    .forEach(kv -> dependencyLoader.loadRepository(kv.getKey(), kv.getValue().getAsString()));
        if (velocityDependency.has("libraries"))
            velocityDependency.getAsJsonArray("libraries")
                    .forEach(v -> dependencyLoader.loadDependency(v.getAsString()));
    }
    @Override
    protected void preInitCore() {
        List<Path> libs = new ArrayList<>();
        List<JsonObject> velocityDependency = new ArrayList<>();
        server.getPluginManager()
                .fromInstance(this)
                .map(PluginContainer::getDescription)
                .flatMap(PluginDescription::getSource)
                .ifPresent(path -> {
                    try (JarInputStream zis = new JarInputStream(new ByteArrayInputStream(Files.readAllBytes(path)))) {
                        JarEntry jarEntry;
                        while (true) {
                            jarEntry = zis.getNextJarEntry();
                            if (jarEntry == null) break;
                            String entryName = jarEntry.getName();
                            if (!jarEntry.isDirectory()) {
                                if (entryName.startsWith("velocity-libs/")) {
                                    Path libPath = Paths.get(dataDirectoryFile, entryName);
                                    Files.createDirectories(libPath.getParent());
                                    Files.copy(zis, libPath, StandardCopyOption.REPLACE_EXISTING);
                                    libs.add(libPath);
                                } else if (entryName.equals("velocity-dependency.json")) {
                                    velocityDependency.add(Json.parse(Charsets.UTF_8
                                                    .decode(ByteBuffer.wrap(zis.readAllBytes()))
                                                    .toString()
                                                    .replace("\r", ""))
                                            .getAsJsonObject());
                                }
                            }
                            zis.closeEntry();
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                });
        var pluginManager = this.server().getPluginManager();
        libs.forEach(lib -> pluginManager.addToClasspath(this, lib));

        Agents.load();

        dependencyLoader = new LibbyVelocityDependencyLoader(this, logger, dataDirectory.resolve("../libby"));

        if (!velocityDependency.isEmpty())
            loadDependencies(velocityDependency.get(0));

        super.preInitCore();
    }
    @Override
    protected void preInitInstance() {
        List<JsonObject> velocityDependency = new ArrayList<>();
        server.getPluginManager()
                .fromInstance(this)
                .map(PluginContainer::getDescription)
                .flatMap(PluginDescription::getSource)
                .ifPresent(path -> {
                    try (JarInputStream zis = new JarInputStream(new ByteArrayInputStream(Files.readAllBytes(path)))) {
                        JarEntry jarEntry;
                        while (true) {
                            jarEntry = zis.getNextJarEntry();
                            if (jarEntry == null) break;
                            String entryName = jarEntry.getName();
                            if (!jarEntry.isDirectory()) {
                                if (entryName.equals("velocity-dependency.json")) {
                                    velocityDependency.add(Json.parse(Charsets.UTF_8
                                                    .decode(ByteBuffer.wrap(zis.readAllBytes()))
                                                    .toString()
                                                    .replace("\r", ""))
                                            .getAsJsonObject());
                                }
                            }
                            zis.closeEntry();
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                });

        dependencyLoader = Objects.requireNonNull(core.dependencyLoader);

        if (!velocityDependency.isEmpty())
            loadDependencies(velocityDependency.get(0));
    }

    @Override
    protected Stream<CoreInstance> globalInstances() {
        return server.getPluginManager()
                .getPlugins()
                .stream()
                .map(v -> v.getInstance().stream())
                .filter(CoreInstance.class::isInstance)
                .map(CoreInstance.class::cast);
    }

    @Override
    protected UnsafeMappings mappings() {
        return VelocityUnsafeMappings.instance();
    }
    @Override
    protected String coreCommandsPostfix() {
        return "-velocity";
    }
}
