package org.lime.core.velocity;

import org.lime.core.common.BaseCoreJarAccess;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface VelocityJarAccess extends BaseCoreJarAccess, VelocityIdentity, VelocityServer {
    @Override
    default Stream<Path> jars() {
        return server()
                .getPluginManager()
                .fromInstance(this)
                .map(v -> v.getDescription())
                .flatMap(v -> v.getSource())
                .stream();
    }
}
