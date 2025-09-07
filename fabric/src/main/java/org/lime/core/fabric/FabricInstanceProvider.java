package org.lime.core.fabric;

import com.google.inject.Provider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.lime.core.common.BaseInstanceProvider;

public abstract class FabricInstanceProvider<Instance extends BaseFabricMod>
        extends BaseInstanceProvider<Instance> {
    static {
        BaseInstanceProvider.setStorage(Storage.of(BaseFabricMod.class, () -> FabricLoader.getInstance()
                .getEntrypointContainers("main", ModInitializer.class)
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
    public static <Instance extends BaseFabricMod, T>Provider<T> getInjectorProvider(Class<Instance> instanceClass, Class<T> type) {
        return getProvider(instanceClass).provider(type);
    }
}
