package org.lime.core.paper;

import com.google.inject.Provider;
import org.bukkit.Bukkit;
import org.lime.core.common.BaseInstanceProvider;

import java.util.Arrays;
import java.util.stream.Stream;

public abstract class PaperInstanceProvider<Instance extends BasePaperInstance<Instance>>
        extends BaseInstanceProvider<Instance> {
    static {
        BaseInstanceProvider.setStorage(Storage.of(BasePaperInstance.class, () -> Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .flatMap(v -> v instanceof BasePaperPlugin paperPlugin ? Stream.of(paperPlugin) : Stream.empty())));
    }

    public static <Instance extends BasePaperInstance<Instance>>PaperInstanceProvider<Instance> getProvider(Class<Instance> instanceClass) {
        return new PaperInstanceProvider<>() {
            @Override
            protected Class<Instance> instanceClass() {
                return instanceClass;
            }
        };
    }
    public static <Instance extends BasePaperInstance<Instance>, T>Provider<T> getInjectorProvider(Class<Instance> instanceClass, Class<T> type) {
        return getProvider(instanceClass).provider(type);
    }
}
