package org.lime.core.paper;

import com.google.inject.Provider;
import org.bukkit.Bukkit;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.BaseInstanceProvider;

import java.util.Arrays;

public abstract class PaperInstanceProvider<Owner extends BasePaperPlugin>
        extends BaseInstanceProvider<Owner> {
    static {
        BaseInstanceProvider.setStorage(Storage.of(BasePaperPlugin.class, () -> Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(BasePaperPlugin.class::isInstance)
                .map(BasePaperPlugin.class::cast)));
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
