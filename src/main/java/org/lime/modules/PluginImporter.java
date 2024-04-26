package org.lime.modules;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.permissions.ServerOperator;
import org.lime.core;
import org.lime.plugin.CoreLoader;
import org.lime.system.json;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PluginImporter {
    private static boolean isEnable = false;
    private static String folder = "../other_plugins";

    public static void register(CoreLoader instance) {
        instance.add("importer.load", v -> v
                .withCheck(ServerOperator::isOp)
                .withTab((s, a) -> a.length == 1 ? List.of("reload") : Collections.emptyList())
                .withExecutor((s,a) -> {
                    load(a.length >= 1 && a[0].equals("reload"));
                    return true;
                }));

        if (!core.instance._existConfig("importer"))
            core.instance._writeAllConfig("importer", json.format(json.object()
                    .add("enable", isEnable)
                    .add("folder", folder)
                    .build()));
    }

    private static void load(boolean isReload) {
        JsonObject v = json.parse(core.instance._readAllConfig("importer")).getAsJsonObject();
        boolean isEnable = v.get("enable").getAsBoolean();
        if (!isEnable) return;
        String folder = v.get("folder").getAsString();
        try {
            File src = new File(folder).getAbsoluteFile();
            File dest = new File("plugins").getAbsoluteFile();
            core.instance._logOP("Copy: " + src + " -> " + dest);
            FileUtils.copyDirectory(src, dest);

            if (isReload) {
                Bukkit.reload();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
