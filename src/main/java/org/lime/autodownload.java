package org.lime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class autodownload implements core.IUpdateConfig, core.ICore {
    public static core.element create() { return new autodownload()._create(); }
    private core.element _create() {
        return core.element.create(autodownload.class)
                .withInstance(this)
                .addEmpty("autodownload-on", () -> enable(true))
                .addEmpty("autodownload-off", () -> enable(false));
    };

    public boolean enable = false;
    public String url = null;
    public core base_core;

    public void syncConfig() {
        JsonObject json = base_core._existConfig("autodownload") ? system.json.parse(base_core._readAllConfig("autodownload")).getAsJsonObject() : new JsonObject();
        enable = json.has("enable") && json.get("enable").getAsBoolean();
        url = enable ? json.get("url").getAsString() : null;
        if (url == null && enable) {
            enable = false;
            base_core._logOP("[AutoDownload] Disable - URL is empty");
        } else {
            base_core._logOP("[AutoDownload] " + (enable ? "Enable" : "Disable"));
        }
    }

    public void enable(boolean state) {
        JsonObject json = base_core._existConfig("autodownload") ? system.json.parse(base_core._readAllConfig("autodownload")).getAsJsonObject() : new JsonObject();
        json.addProperty("enable", state);
        base_core._writeAllConfig("autodownload", system.toFormat(json));
        updateConfigAsync(Collections.emptyList(), () -> {});
    }
    @Override public void updateConfigAsync(Collection<String> files, system.Action0 callback) {
        syncConfig();
        base_core._logOP("UPDATE: " + (files == null ? "ALL" : String.join(", ", files)));
        if (enable) downloadConfigFiles(files, callback);
        else callback.invoke();
    }

    @Override public void core(core base_core) {
        this.base_core = base_core;
        this.base_core._logOP("Loaded 'autodownload' module!");
    }
    @Override public core core() { return base_core; }

    @Override public void updateConfigSync() {
        syncConfig();
        base_core._logOP("UPDATE: ALL");
        if (!enable) return;
        downloadConfigFiles(null);
    }

    private void downloadConfigFiles(Collection<String> fileList, system.Action0 callback) {
        base_core._invokeAsync(() -> downloadConfigFiles(fileList), callback);
    }
    private void downloadConfigFiles(Collection<String> fileList) {
        base_core._logOP("Downloading...");
        system.Toast2<byte[], Integer> downloaded = web.method.GET.create(url).data().execute();
        base_core._logOP("Opening...");
        Map<String, byte[]> files;
        try {
            files = zip.unzip(downloaded.val0);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error download: " + e.toString() + " with code '" + downloaded.val1 + "' with data '" + new String(downloaded.val0) + "'", e);
        }
        base_core._logOP("Reading...");
        for (Map.Entry<String, JsonElement> kv : new JsonParser().parse(new String(files.get("link.json"))).getAsJsonObject().entrySet()) {
            if (!(fileList == null || fileList.contains(kv.getKey()) || (!kv.getKey().endsWith(".json") && fileList.contains(kv.getKey() + ".json")))) continue;
            byte[] bytes = files.get(kv.getValue().getAsString());
            base_core._logOP(" - " + kv.getValue().getAsString() + " : " + bytes.length + "B");
            String[] file = kv.getKey().split("\\.");
            if (file.length == 1) base_core._writeAllConfig(file[0], StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString());
            else base_core._writeAllConfig(file[0], "." + file[1], StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString());
        }
        base_core._logOP("Downloaded!");
    }
}














