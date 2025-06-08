package org.lime.core.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.api.BaseIdentity;

import java.util.Optional;

public interface VelocityIdentity extends BaseIdentity {
    String pluginId();

    static Optional<VelocityIdentity> of(ProxyServer server, Object plugin) {
        return server.getPluginManager()
                .fromInstance(plugin)
                .map(v -> v.getDescription())
                .map(v -> new VelocityIdentity() {
                    @Override
                    public String pluginId() {
                        return v.getId();
                    }
                    @Override
                    public String name() {
                        return v.getName().orElseGet(v::getId);
                    }
                });
    }
}
