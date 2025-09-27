package org.lime.core.fabric;

import com.google.inject.Provider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.BaseInstanceProvider;

import java.util.stream.Stream;

public abstract class FabricInstanceProvider<Instance extends BaseFabricMod>
        extends BaseInstanceProvider<Instance> {
    private static final Storage<BaseFabricMod> storage;
    static {
        BaseInstanceProvider.setStorage(storage = Storage.of(BaseFabricMod.class, CoreFabricMod.class, () -> FabricLoader.getInstance()
                .getEntrypointContainers("main", ModInitializer.class)
                .stream()
                .map(EntrypointContainer::getEntrypoint)
                .filter(BaseFabricMod.class::isInstance)
                .map(BaseFabricMod.class::cast)));
    }

    public static BaseFabricMod getCore() {
        return storage.getCore();
    }
    public static Stream<? extends BaseFabricMod> getOwners() {
        return storage.getOwners();
    }
    public static <Owner extends BaseFabricMod>FabricInstanceProvider<Owner> getProvider(Class<Owner> ownerClass) {
        return new FabricInstanceProvider<>() {
            @Override
            protected Class<Owner> ownerClass() {
                return ownerClass;
            }
            @Override
            protected BaseInstance<?> instance(Owner owner) {
                return owner;
            }
        };
    }
    public static <Instance extends BaseFabricMod, T>Provider<T> getInjectorProvider(Class<Instance> ownerClass, Class<T> type) {
        return getProvider(ownerClass).provider(type);
    }
}
