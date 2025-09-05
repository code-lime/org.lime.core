package org.lime.core.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.lime.core.common.BaseInstanceProvider;

public abstract class FabricInstanceProvider<Instance extends BaseFabricMod>
        extends BaseInstanceProvider<Instance> {
    static {
        BaseInstanceProvider.setStorage(Storage.of(BaseFabricMod.class, () -> FabricLoader.getInstance()
                .getEntrypointContainers("main", DedicatedServerModInitializer.class)
                .stream()
                .map(EntrypointContainer::getEntrypoint)));
    }

    public static <Instance extends BaseFabricMod>FabricInstanceProvider<Instance> getProvider(Class<Instance> instanceClass) {
        return new FabricInstanceProvider<>() {
            @Override
            protected Class<Instance> instanceClass() {
                return instanceClass;
            }
        };
    }
}
