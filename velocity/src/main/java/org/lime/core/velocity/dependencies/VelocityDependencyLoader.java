package org.lime.core.velocity.dependencies;

import com.google.gson.JsonElement;

public interface VelocityDependencyLoader {
    void loadRepository(String id, JsonElement repository);
    void loadDependency(String dependency);
}
