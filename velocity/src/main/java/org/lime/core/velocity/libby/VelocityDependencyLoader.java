package org.lime.core.velocity.libby;

public interface VelocityDependencyLoader {
    void loadRepository(String name, String url);
    void loadDependency(String dependency);
}
