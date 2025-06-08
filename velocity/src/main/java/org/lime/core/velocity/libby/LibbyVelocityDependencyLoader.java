package org.lime.core.velocity.libby;

import com.alessiodp.libby.Library;
import com.alessiodp.libby.VelocityLibraryManager;
import org.lime.core.velocity.CoreInstance;
import org.lime.core.velocity.VelocityDependencyLoader;
import org.slf4j.Logger;

import java.nio.file.Path;

public class LibbyVelocityDependencyLoader
        implements VelocityDependencyLoader {
    private final VelocityLibraryManager<? extends CoreInstance> libraryManager;
    public LibbyVelocityDependencyLoader(
            CoreInstance plugin,
            Logger logger,
            Path dataDirectory) {
        this.libraryManager = new VelocityLibraryManager<>(plugin, logger, dataDirectory, plugin.server().getPluginManager());
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
