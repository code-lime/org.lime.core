package net.minecraft.paper.java;

import io.papermc.paper.plugin.configuration.PluginMeta;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public interface RawPluginMeta extends PluginMeta {
    Map<?,?> rawData();
    void rawData(Map<?,?> raw);
}
