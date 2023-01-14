package org.lime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

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
    public HashMap<String, String> headers = new HashMap<>();
    public Pattern path = null;
    public List<Pattern> ignore = new ArrayList<>();
    public core base_core;

    public void syncConfig() {
        JsonObject json = base_core._existConfig("autodownload") ? system.json.parse(base_core._readAllConfig("autodownload")).getAsJsonObject() : new JsonObject();
        enable = json.has("enable") && json.get("enable").getAsBoolean();
        url = enable ? json.get("url").getAsString() : null;

        headers.clear();
        if (json.has("headers"))
            json.getAsJsonObject("headers")
                .entrySet()
                .forEach(kv -> headers.put(kv.getKey(), kv.getValue().getAsString()));

        ignore.clear();
        if (json.has("ignore"))
            json.getAsJsonArray("ignore")
                .forEach(kv -> ignore.add(Pattern.compile(kv.getAsString(), Pattern.CASE_INSENSITIVE)));

        path = enable && json.has("path") ? Pattern.compile(json.get("path").getAsString()) : Pattern.compile("");
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
        web.method.builder builder = web.method.GET.create(url);
        headers.forEach(builder::header);
        system.Toast2<byte[], Integer> downloaded = builder.data().execute();
        base_core._logOP("Opening...");
        Map<String, byte[]> files;
        try {
            Map<String, byte[]> _files = zip.unzip(downloaded.val0);
            files = new HashMap<>();
            Map<String, JsonObject> dirs = new HashMap<>();
            _files.forEach((key,value) -> {
                String __path = String.join("", path.split(key));
                for (Pattern pattern : ignore) {
                    if (pattern.matcher(__path).find())
                        return;
                }
                base_core._logOP("File path: " + __path);
                String[] _path = __path.split("/", 2);
                if (_path.length > 1) {
                    dirs.compute(_path[0], (k,json) -> {
                        if (json == null) json = new JsonObject();
                        json = base_core._combineJson(json, system.json.parse(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(value)).toString()), false).getAsJsonObject();
                        return json;
                    });
                } 
                else files.put(_path[0], value);
            });
            dirs.forEach((key,value) -> files.put(key + ".json", value.toString().getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error download: " + e.toString() + " with code '" + downloaded.val1 + "' with data '" + /*new String(downloaded.val0)*/"..." + "'", e);
        }

        base_core._logOP("Reading...");
        for (Map.Entry<String, JsonElement> kv : system.json.parse(new String(files.get("link.json"))).getAsJsonObject().entrySet()) {
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














