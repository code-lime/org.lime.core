package org.lime.core.velocity;

public interface VelocityDependencyLoader {
    void loadRepository(String name, String url);
    void loadDependency(String dependency);
}
