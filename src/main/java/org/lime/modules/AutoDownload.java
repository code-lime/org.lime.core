package org.lime.modules;

import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lime.LimeCore;
import org.lime.plugin.CoreElement;
import org.lime.plugin.ICore;
import org.lime.plugin.IUpdateConfig;
import org.lime.system.execute.Action0;
import org.lime.json.builder.Json;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple2;
import org.lime.Web;
import org.lime.Zip;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoDownload implements IUpdateConfig, ICore {
    public static CoreElement create() { return new AutoDownload()._create(); }
    private CoreElement _create() {
        return CoreElement.create(AutoDownload.class)
                .withInstance(this)
                .addEmpty("autodownload-on", () -> enable(true))
                .addEmpty("autodownload-off", () -> enable(false));
    }

    public boolean enable = false;
    public String ref = null;
    public String url = null;
    public HashMap<String, String> headers = new HashMap<>();
    public Pattern path = null;
    public List<Pattern> ignore = new ArrayList<>();
    public LimeCore base_core;

    public void syncConfig() {
        JsonObject json = base_core._existConfig("autodownload") ? Json.parse(base_core._readAllConfig("autodownload")).getAsJsonObject() : new JsonObject();
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

        ref = enable && json.has("ref") ? json.get("ref").getAsString() : null;

        path = enable && json.has("path") ? Pattern.compile(json.get("path").getAsString()) : Pattern.compile("");
        if (url == null && enable) {
            enable = false;
            base_core._logOP("[AutoDownload] Disable - URL is empty");
        } else {
            base_core._logOP("[AutoDownload] " + (enable ? "Enable" : "Disable"));
        }
    }

    public void enable(boolean state) {
        JsonObject json = base_core._existConfig("autodownload") ? Json.parse(base_core._readAllConfig("autodownload")).getAsJsonObject() : new JsonObject();
        json.addProperty("enable", state);
        base_core._writeAllConfig("autodownload", Json.format(json));
        updateConfigAsync(Collections.emptyList(), () -> {});
    }
    @Override public void updateConfigAsync(Collection<String> files, Action0 callback) {
        syncConfig();
        base_core._logOP("UPDATE: " + (files == null ? "ALL" : String.join(", ", files)));
        if (enable) downloadConfigFiles(files, callback);
        else callback.invoke();
    }

    @Override public void core(LimeCore base_core) {
        this.base_core = base_core;
        this.base_core._logOP("Loaded 'autodownload' module!");
    }
    @Override public LimeCore core() { return base_core; }

    @Override public void updateConfigSync() {
        syncConfig();
        base_core._logOP("UPDATE: ALL");
        if (!enable) return;
        downloadConfigFiles(null);
    }

    private void downloadConfigFiles(Collection<String> fileList, Action0 callback) {
        base_core._invokeAsync(() -> downloadConfigFiles(fileList), callback);
    }

    private Stream<File> getFilesWithoutGit(File folder) {
        return Optional.ofNullable(folder.listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .flatMap(v -> v.isDirectory() ? v.getName().equals(".git") ? Stream.empty() : getFilesWithoutGit(v) : Stream.of(v));
    }

    private Map<String, byte[]> downloadRawFiles(final Tuple2<byte[], Integer> downloaded) {
        if (url.startsWith("folder://")) {
            base_core._logOP("Reading...");
            File folder = new File(url.substring(9));
            Path folderPath = folder.toPath();
            Map<String, byte[]> map = new HashMap<>();
            //var gitignore = new GitIgnore(folder);
            getFilesWithoutGit(folder)
                    .forEach(file -> {
                        String path = String.valueOf(folderPath.relativize(file.toPath()));
                        /*if (gitignore.isExcluded(file)) return;*/
                        String fileName = "tmp/" + path;
                        try {
                            map.put(fileName, FileUtils.readFileToByteArray(file));
                        } catch (Exception e) {
                            throw new IllegalArgumentException(e);
                        }
                    });
            return map;
        }
        base_core._logOP("Downloading...");
        Web.Method.Builder builder = Web.Method.GET.create(url);
        headers.forEach(builder::header);
        downloaded.set(builder.data().execute());
        base_core._logOP("Opening...");
        return Zip.unzip(downloaded.val0);
    }

    private void downloadConfigFiles(Collection<String> fileList) {
        Tuple2<byte[], Integer> downloaded = Tuple.of(new byte[0], -1);
        Map<String, byte[]> files;
        try {
            Map<String, byte[]> _files = downloadRawFiles(downloaded);
            files = new HashMap<>();
            Map<String, JsonObject> jsonDirs = new HashMap<>();
            Map<String, StringBuilder> jsDirs = new HashMap<>();

            List<String> loadList = new ArrayList<>();

            Set<String> refs = _files.keySet()
                    .stream()
                    .map(path -> String.join("", this.path.split(path)))
                    .filter(path -> ignore.stream().noneMatch(pattern -> pattern.matcher(path).find()))
                    .map(path -> path.split("/"))
                    .filter(path -> path.length >= 3 && path[0].equals("ref"))
                    .map(path -> path[1])
                    .collect(Collectors.toSet());

            String currentRef = refs.contains(ref) ? ref : ".default";
            String currentRefPath = "ref/" + currentRef + "/";
            base_core._logOP("Current: " + currentRefPath);

            Map<String, byte[]> _resultFiles = new HashMap<>(_files);
            _resultFiles.keySet().removeIf(v -> String.join("", this.path.split(v)).startsWith("ref/"));
            _files.forEach((k,v) -> {
                String key = String.join("", this.path.split(k));
                if (key.startsWith("ref/") && key.startsWith(currentRefPath)) {
                    _resultFiles.put(k.replace("/" + currentRefPath, "/"), v);
                }
            });
            _resultFiles.entrySet()
                    .stream()
                    .map(kv -> Tuple.of(String.join("", path.split(kv.getKey())), kv.getValue()))
                    .filter(kv -> ignore.stream().noneMatch(pattern -> pattern.matcher(kv.val0).find()))
                    .map(kv -> Tuple.of(kv.val0.split("/"), kv.val1))
                    .sorted(Comparator.comparing(kv -> kv.val0[kv.val0.length - 1]))
                    .forEach(kv -> kv.invoke((path, bytes) -> {
                        if (path.length > 1) {
                            String ext = FilenameUtils.getExtension(path[path.length - 1]);
                            loadList.add("[D] " + path[0] + " / " + String.join("/", path));
                            switch (ext) {
                                case "json" -> jsonDirs.compute(path[0], (k, json) -> {
                                    if (json == null) json = new JsonObject();
                                    JsonObject item = Json.parse(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString()).getAsJsonObject();
                                    json = base_core._combineJson(json, item, false).getAsJsonObject();
                                    return json;
                                });
                                case "js", "txt" -> jsDirs.compute(path[0], (k, text) -> {
                                    if (text == null) text = new StringBuilder();
                                    text.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes))).append("\n");
                                    return text;
                                });
                                default ->
                                        throw new IllegalArgumentException("Not supported extension '" + ext + "' of file '" + String.join("/", path) + "'");
                            }
                        }
                        else {
                            loadList.add("[F] " + path[0] + " / " + String.join("/", path));
                            files.put(path[0], bytes);
                        }
                    }));
/*
            _files.entrySet()
                    .stream()
                    .map(kv -> Tuple.of(String.join("", path.split(kv.getKey())), kv.getValue()))
                    .filter(kv -> ignore.stream().noneMatch(pattern -> pattern.matcher(kv.val0).find()))
                    .map(kv -> Tuple.of(kv.val0.split("/"), kv.val1))
                    .sorted(Comparator.comparing(kv -> kv.val0[kv.val0.length - 1]))
                    .filter(kv -> !kv.val0[0].equals("ref") || kv.val0.length >= 3 && kv.val0[1].equals(currentRef))
                    .map(kv -> kv.val0[0].equals("ref") ? Tuple.of(Arrays.stream(kv.val0).skip(2).toArray(String[]::new), kv.val1) : kv)
                    .forEach(kv -> kv.invoke((path, bytes) -> {
                        if (path.length > 1) {
                            String ext = FilenameUtils.getExtension(path[path.length - 1]);
                            loadList.add("[D] " + path[0] + " / " + String.join("/", path));
                            switch (ext) {
                                case "json" -> jsonDirs.compute(path[0], (k, json) -> {
                                    if (json == null) json = new JsonObject();
                                    JsonObject item = org.lime.system.json.parse(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString()).getAsJsonObject();
                                    json = base_core._combineJson(json, item, false).getAsJsonObject();
                                    return json;
                                });
                                case "js", "txt" -> jsDirs.compute(path[0], (k, text) -> {
                                    if (text == null) text = new StringBuilder();
                                    text.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes))).append("\n");
                                    return text;
                                });
                                default ->
                                        throw new IllegalArgumentException("Not supported extension '" + ext + "' of file '" + String.join("/", path) + "'");
                            }
                        }
                        else {
                            loadList.add("[F] " + path[0] + " / " + String.join("/", path));
                            files.put(path[0], bytes);
                        }
                    }));
*/
            //
            base_core._logOP(Component.text("Load list: [view]")
                .hoverEvent(HoverEvent.showText(Component.text(String.join("\n", loadList)))));
            jsonDirs.forEach((key,value) -> files.put(key + ".json", value.toString().getBytes()));
            jsDirs.forEach((key,value) -> files.put(key + ".js", value.toString().getBytes()));
        } catch (Exception e) {
            try { Files.write(downloaded.val0, base_core._getConfigFile("autodownload-error.zip")); }
            catch (Exception ignore) { }
            throw new IllegalArgumentException("Error download: " + e.toString() + " with code '" + downloaded.val1 + "' with data '" + /*new String(downloaded.val0)*/"..." + "'", e);
        }

        base_core._logOP("Reading...");
        for (Map.Entry<String, JsonElement> kv : Json.parse(new String(files.get("link.json"))).getAsJsonObject().entrySet()) {
            if (!(fileList == null || fileList.contains(kv.getKey()) || (!kv.getKey().endsWith(".json") && fileList.contains(kv.getKey() + ".json")))) continue;
            byte[] bytes = files.get(kv.getValue().getAsString());
            if (bytes == null)
                throw new IllegalArgumentException("File '"+kv.getValue().getAsString() + "' not found");
            base_core._logOP(" - " + kv.getValue().getAsString() + " : " + bytes.length + "B");
            String[] file = kv.getKey().split("\\.");
            if (file.length == 1) base_core._writeAllConfig(file[0], StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString());
            else base_core._writeAllConfig(file[0], "." + file[1], StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString());
        }
        base_core._logOP("Downloaded!");
    }
}














