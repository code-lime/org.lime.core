package org.lime.core.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.lime.core.common.BaseCoreJarAccess;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface FabricJarAccess extends BaseCoreJarAccess, FabricIdentity {
    @Override
    default Stream<Path> jars() {
        return FabricLoader.getInstance()
                .getModContainer(modId())
                .orElseThrow()
                .getOrigin()
                .getPaths()
                .stream();
    }
}
