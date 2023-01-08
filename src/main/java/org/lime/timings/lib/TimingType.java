package org.lime.timings.lib;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

enum TimingType {
    @Deprecated
    SPIGOT(true) {
        @Override
        MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
            return new SpigotTiming(command);
        }
    },
    MINECRAFT() {
        @Override
        MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
            return new MinecraftTiming(plugin, command, parent);
        }
    },
    MINECRAFT_18() {
        @Override
        MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
            try {
                return new Minecraft18Timing(plugin, command, parent);
            } catch (InvocationTargetException | IllegalAccessException e) {
                return new EmptyTiming();
            }
        }
    },
    EMPTY();

    private final boolean useCache;

    public boolean useCache() {
        return useCache;
    }

    TimingType() {
        this(false);
    }
    TimingType(boolean useCache) {
        this.useCache = useCache;
    }

    MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
        return new EmptyTiming();
    }
}
