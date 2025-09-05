package org.lime.core.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.agent.Agents;
import org.lime.core.velocity.libby.LibbyVelocityDependencyLoader;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        id = BuildConstants.ID,
        name = BuildConstants.NAME,
        version = BuildConstants.VERSION,
        authors = "Lime"
)
public final class CoreVelocityPlugin
        extends BaseVelocityPlugin {
    @Inject
    public CoreVelocityPlugin(ProxyServer server, Logger logger, Path dataDirectory) {
        super(server, logger, dataDirectory);
        VelocityInstanceProvider.proxyServer = server;
    }

    @Override
    protected BaseVelocityInstanceModule createModule() {
        return new BaseVelocityInstanceModule(this);
    }

    @Override
    public void enable() {
        List<Path> libs = new ArrayList<>();
        analyzePluginJar((entry, stream) -> {
            String entryName = entry.getName();
            if (!entryName.startsWith("velocity-libs/"))
                return;

            Path libPath = dataFolder.toPath().resolve(entryName);
            Files.createDirectories(libPath.getParent());
            Files.copy(stream, libPath, StandardCopyOption.REPLACE_EXISTING);
            libs.add(libPath);
        });
        PluginManager pluginManager = this.server.getPluginManager();
        libs.forEach(lib -> pluginManager.addToClasspath(this, lib));

        Agents.load();

        globalDependencyLoader = new LibbyVelocityDependencyLoader(this, logger, dataFolder.toPath().resolve("../libby"));

        super.enable();
    }
    @Override
    public void disable() {
        super.disable();
        Agents.unload();
    }
}
