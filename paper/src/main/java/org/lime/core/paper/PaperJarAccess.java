package org.lime.core.paper;

import org.lime.core.common.BaseCoreJarAccess;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface PaperJarAccess extends BaseCoreJarAccess, PaperIdentity {
    File pluginFile();

    @Override
    default Stream<Path> jars() {
        return Stream.of(pluginFile().toPath());
    }
}
