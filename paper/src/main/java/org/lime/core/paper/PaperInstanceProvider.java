package org.lime.core.paper;

import com.google.inject.Provider;
import org.bukkit.Bukkit;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.BaseInstanceProvider;

import java.util.Arrays;
import java.util.stream.Stream;

public abstract class PaperInstanceProvider<Owner extends BasePaperPlugin>
        extends BaseInstanceProvider<Owner> {
    private static final Storage<BasePaperPlugin> storage;
    static {
        BaseInstanceProvider.setStorage(storage = Storage.of(BasePaperPlugin.class, CorePaperPlugin.class, () -> Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(BasePaperPlugin.class::isInstance)
                .map(BasePaperPlugin.class::cast)));
    }

    public static BasePaperPlugin getCore() {
        return storage.getCore();
    }
    public static Stream<? extends BasePaperPlugin> getOwners() {
        return storage.getOwners();
    }
    public static <Owner extends BasePaperPlugin>PaperInstanceProvider<Owner> getProvider(Class<Owner> ownerClass) {
        return new PaperInstanceProvider<>() {
            @Override
            protected Class<Owner> ownerClass() {
                return ownerClass;
            }
            @Override
            protected BaseInstance<?> instance(Owner owner) {
                return owner.instance;
            }
        };
    }
    public static <Owner extends BasePaperPlugin, T>Provider<T> getInjectorProvider(Class<Owner> ownerClass, Class<T> type) {
        return getProvider(ownerClass).provider(type);
    }
}
