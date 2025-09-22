package org.lime.core.velocity;

import com.google.inject.Provider;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.BaseInstanceProvider;

import java.util.Optional;

public abstract class VelocityInstanceProvider<Owner extends BaseVelocityPlugin>
        extends BaseInstanceProvider<Owner> {
    static ProxyServer proxyServer;
    static {
        BaseInstanceProvider.setStorage(Storage.of(BaseVelocityPlugin.class, () -> proxyServer.getPluginManager()
                .getPlugins()
                .stream()
                .map(PluginContainer::getInstance)
                .flatMap(Optional::stream)
                .filter(BaseVelocityPlugin.class::isInstance)
                .map(BaseVelocityPlugin.class::cast)));
    }

    public static <Owner extends BaseVelocityPlugin>VelocityInstanceProvider<Owner> getProvider(Class<Owner> instanceClass) {
        return new VelocityInstanceProvider<>() {
            @Override
            protected Class<Owner> ownerClass() {
                return instanceClass;
            }
            @Override
            protected BaseInstance<?> instance(Owner owner) {
                return owner;
            }
        };
    }
    public static <Owner extends BaseVelocityPlugin, T>Provider<T> getInjectorProvider(Class<Owner> instanceClass, Class<T> type) {
        return getProvider(instanceClass).provider(type);
    }
}
