package patch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;
import org.lime.system.Regex;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.utils.IterableUtils;

import javax.annotation.Nullable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class BasePluginPatcher implements SignatureTools {
    public final String name;
    public final Class<? extends JavaPlugin> plugin;
    public final JsonObject patchData;
    private JarArchive pluginArchive = null;

    private static byte[] readPatchResource(Class<? extends JavaPlugin> plugin) {
        try {
            URL url = plugin.getClassLoader().getResource("patch.json");
            if (url == null)
                return null;
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream().readAllBytes();
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

    public BasePluginPatcher(Class<? extends JavaPlugin> plugin) {
        this(plugin, readPatchResource(plugin));
    }
    private BasePluginPatcher(Class<? extends JavaPlugin> plugin, @Nullable byte[] patchData) {
        this(plugin, patchData == null ? new JsonObject() : json.parse(new String(patchData)).getAsJsonObject());
    }
    private BasePluginPatcher(Class<? extends JavaPlugin> plugin, JsonObject patchData) {
        this.plugin = plugin;
        this.name = plugin.getName();
        this.patchData = patchData;
    }

    public JarArchive pluginArchive() {
        return Objects.requireNonNull(pluginArchive);
    }
    public void pluginArchive(JarArchive pluginArchive) {
        this.pluginArchive = pluginArchive;
    }

    public List<ModifyClass> modifyList() {
        List<ModifyClass> classes = new ArrayList<>();
        if (patchData.has("append")) {
            patchData.getAsJsonArray("append")
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .forEach(name -> {
                        Native.log("Append single " + name);
                        classes.add(new ModifyClass(name, pluginArchive.entries.get(name), true));
                    });
        }
        if (patchData.has("append_regex")) {
            Stream.concat(patchData.getAsJsonArray("append_regex")
                                    .asList()
                                    .stream()
                                    .map(JsonElement::getAsString)
                                    .map(v -> Toast.of(v, true)),
                            Stream.of(Toast.of("patch\\/.*", false)))
                    .filter(IterableUtils.distinctBy(v -> v.val0))
                    .forEach(v -> v.invoke((regex, patch) -> Native.subLog("Append group '"+regex+"'", () -> pluginArchive.entries
                            .entrySet()
                            .stream()
                            .filter(kv -> Regex.compareRegex(kv.getKey(), regex))
                            .forEach(kv -> {
                                //Native.log(kv.getKey() + " ["+(patch ? "PATCH" : "CHECK")+"]");
                                classes.add(new ModifyClass(kv.getKey(), kv.getValue(), patch));
                            }))));
        }
        return classes;
    }

    public abstract void patch(JarArchive versionArchive, JarArchive bukkitArchive);
}