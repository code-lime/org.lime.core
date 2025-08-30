package org.lime.core.velocity.libby;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.VelocityLibraryManager;
import org.lime.core.velocity.CoreVelocityPlugin;
import org.slf4j.Logger;

import java.nio.file.Path;

public class LibbyVelocityDependencyLoader
        implements VelocityDependencyLoader {
    private final VelocityLibraryManager<? extends CoreVelocityPlugin> libraryManager;
    public LibbyVelocityDependencyLoader(
            CoreVelocityPlugin plugin,
            Logger logger,
            Path dataDirectory) {
        this.libraryManager = new VelocityLibraryManager<>(plugin, logger, dataDirectory, plugin.server.getPluginManager());
        this.libraryManager.addMavenCentral();
    }

    @Override
    public void loadRepository(String name, String url) {
        this.libraryManager.addRepository(url);
    }
    @Override
    public void loadDependency(String dependency) {
        String[] args = dependency.split(":");
        Library.Builder builder = Library.builder();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (i) {
                case 0 -> builder.groupId(arg);
                case 1 -> builder.artifactId(arg);
                case 2 -> builder.version(arg);
            }
        }
        this.libraryManager.loadLibrary(builder.build());
    }
}
