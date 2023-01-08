package org.lime.timings.lib;

import co.aikar.timings.Timings;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class TimingManager {

    private static TimingType timingProvider;
    private static final Object LOCK = new Object();

    private final Plugin plugin;
    private final Map<String, MCTiming> timingCache = new HashMap<>(0);

    private TimingManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("WeakerAccess")
    public static TimingManager of(Plugin plugin) {
        return new TimingManager(plugin);
    }

    @SuppressWarnings("WeakerAccess")
    public MCTiming ofStart(String name) {
        return ofStart(name, null);
    }

    @SuppressWarnings("WeakerAccess")
    public MCTiming ofStart(String name, MCTiming parent) {
        return of(name, parent).startTiming();
    }

    @SuppressWarnings("WeakerAccess")
    public MCTiming of(String name) {
        return of(name, null);
    }

    @SuppressWarnings({"WeakerAccess","deprecation"})
    public MCTiming of(String name, MCTiming parent) {
        if (timingProvider == null) {
            synchronized (LOCK) {
                if (timingProvider == null) {
                    try {
                        Class<?> clazz = Class.forName("co.aikar.timings.Timing");
                        Method startTiming = clazz.getMethod("startTiming");
                        if (startTiming.getReturnType() != clazz) {
                            timingProvider = TimingType.MINECRAFT_18;
                        } else {
                            timingProvider = TimingType.MINECRAFT;
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException ignored1) {
                        try {
                            Class.forName("org.spigotmc.CustomTimingsHandler");
                            timingProvider = TimingType.SPIGOT;
                        } catch (ClassNotFoundException ignored2) {
                            timingProvider = TimingType.EMPTY;
                        }
                    }
                }
            }
        }

        MCTiming timing;
        if (timingProvider.useCache()) {
            synchronized (timingCache) {
                String lowerKey = name.toLowerCase();
                timing = timingCache.get(lowerKey);
                if (timing == null) {
                    timing = timingProvider.newTiming(plugin, name, parent);
                    timingCache.put(lowerKey, timing);
                }
            }
            return timing;
        }

        return timingProvider.newTiming(plugin, name, parent);
    }
}
