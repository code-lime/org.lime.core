package org.lime.core.paper.modules;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.lime.core.common.json.builder.Json;
import org.lime.core.paper.CoreInstancePlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PluginImporter {
    private static boolean isEnable = false;
    private static String folder = "../other_plugins";

    public static void register(CoreInstancePlugin.CoreInstance instance) {
        instance.addCommand("importer.load", v -> v
                .addOperatorOnly()
                .withTab((s, a) -> a.length == 1 ? List.of("reload") : Collections.emptyList())
                .withExecutor((s,a) -> {
                    load(instance, a.length >= 1 && a[0].equals("reload"));
                    return true;
                }));

        if (!instance.$existConfig("importer"))
            instance.$writeAllConfig("importer", Json.format(Json.object()
                    .add("enable", isEnable)
                    .add("folder", folder)
                    .build()));
    }

    private static void load(CoreInstancePlugin.CoreInstance instance, boolean isReload) {
        JsonObject v = Json.parse(instance.$readAllConfig("importer")).getAsJsonObject();
        boolean isEnable = v.get("enable").getAsBoolean();
        if (!isEnable) return;
        String folder = v.get("folder").getAsString();
        try {
            File src = new File(folder).getAbsoluteFile();
            File dest = new File("plugins").getAbsoluteFile();
            instance.$logOP("Copy: " + src + " -> " + dest);
            FileUtils.copyDirectory(src, dest);

            if (isReload) {
                Bukkit.reload();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
