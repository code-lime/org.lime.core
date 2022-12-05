package org.lime.timings.lib;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.bukkit.plugin.Plugin;

class MinecraftTiming extends MCTiming {
    private final Timing timing;
    MinecraftTiming(Plugin plugin, String name, MCTiming parent) {
        this.timing = Timings.of(plugin, name, parent instanceof MinecraftTiming ? ((MinecraftTiming) parent).timing : null);
    }
    @Override public MCTiming startTiming() { timing.startTimingIfSync(); return this; }
    @Override public void stopTiming() { timing.stopTimingIfSync(); }
}
