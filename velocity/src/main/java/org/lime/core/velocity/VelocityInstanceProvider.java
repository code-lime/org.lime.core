package org.lime.core.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.BaseInstanceProvider;

import java.util.Optional;

public abstract class VelocityInstanceProvider<Instance extends BaseVelocityPlugin>
        extends BaseInstanceProvider<Instance> {
    static ProxyServer proxyServer;
    static {
        BaseInstanceProvider.setStorage(Storage.of(BaseVelocityPlugin.class, () -> proxyServer.getPluginManager()
                .getPlugins()
                .stream()
                .map(PluginContainer::getInstance)
                .flatMap(Optional::stream)));
    }

    public static <Instance extends BaseVelocityPlugin>VelocityInstanceProvider<Instance> getProvider(Class<Instance> instanceClass) {
        return new VelocityInstanceProvider<>() {
            @Override
            protected Class<Instance> instanceClass() {
                return instanceClass;
            }
        };
    }
}
