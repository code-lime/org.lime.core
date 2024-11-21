package org.lime.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.LimeCore;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;
import org.lime.json.builder.Json;

import java.util.ArrayList;
import java.util.List;

public class CoreData<T> {
    private final Type type;

    private enum Type {
        Text(false),
        Json(false),
        None(false),
        Init(false);

        public final boolean init;

        Type(boolean init) {
            this.init = init;
        }
    }

    public final String name;
    public final String parent;
    public final String file;
    public final CoreData<?> if_empty;
    public final Action1<T> invoke;
    public final Func0<T> def;

    public final Func1<String, T> read;
    public final Func1<T, String> write;

    private CoreData(Type type, String file, String name, String parent, Action1<T> invoke, Func0<T> def, Func1<String, T> read, Func1<T, String> write, CoreData<?> if_empty) {
        this.type = type;
        this.file = file;
        this.name = name;
        this.parent = parent;
        this.invoke = invoke;
        this.def = def;
        this.if_empty = if_empty;

        this.read = read;
        this.write = write;
    }

    private static <T> CoreData<T> create(Type type, Func1<String, T> read, Func1<T, String> write) {
        return new CoreData<>(type, null, null, null, null, null, read, write, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> CoreData<T> json() {
        return create(Type.Json, text -> (T) Json.parse(text), Json::format);
    }

    public static CoreData<String> text() {
        return create(Type.Text, v -> v, v -> v);
    }

    static CoreData<Object> none() {
        return create(Type.None, null, null);
    }

    static CoreData<Object> init() {
        return create(Type.Init, null, null);
    }

    public CoreData<T> withInvoke(Action1<T> invoke) {
        return new CoreData<>(type, file, name, parent, invoke, def, read, write, if_empty);
    }

    public CoreData<T> withDefault(Func0<T> def) {
        return new CoreData<>(type, file, name, parent, invoke, def, read, write, if_empty);
    }

    public CoreData<T> withDefault(T def) {
        return withDefault(() -> def);
    }

    public CoreData<T> withParent(String parent) {
        return new CoreData<>(type, file, name, parent, invoke, def, read, write, if_empty);
    }


    public CoreData<T> orText(String file, Func1<CoreData<String>, CoreData<String>> builder) {
        return orFile(file, file, builder.invoke(CoreData.text()));
    }

    public <T2 extends JsonElement> CoreData<T> orConfig(String config, Func1<CoreData<T2>, CoreData<T2>> builder) {
        return orFile(config + ".json", config, builder.invoke(CoreData.json()));
    }

    public CoreData<T> orFile(String file, String name, CoreData<?> data) {
        return new CoreData<>(type, this.file, this.name, parent, invoke, def, read, write, data.withFile(file).withName(name));
    }

    CoreData<T> withFile(String file) { return new CoreData<>(type, file, name, parent, invoke, def, read, write, if_empty); }
    CoreData<T> withName(String name) { return new CoreData<>(type, file, name, parent, invoke, def, read, write, if_empty); }

    private void invokeRead(String text) {
        invoke.invoke(read.invoke(text));
    }

    private String getDefault() {
        return def == null ? "" : write.invoke(def.invoke());
    }

    List<String> getFiles() {
        List<String> list = new ArrayList<>();
        list.add(name == null ? file : name);
        if (if_empty != null) list.addAll(if_empty.getFiles());
        return list;
    }

    void read(LimeCore plugin, boolean update) {
        if (read == null || write == null) {
            if (update || type.init) invoke.invoke(null);
            return;
        }
        String[] split = file.split("\\.");
        String _file = split[0];
        String ext = "." + split[1];

        boolean isExist = plugin._existConfig(_file, ext);

        if (!isExist && if_empty != null) {
            if_empty.read(plugin, update);
            return;
        }

        if (parent == null) {
            if (!isExist) plugin._writeAllConfig(_file, ext, getDefault());
            invokeRead(plugin._readAllConfig(_file, ext));
        } else {
            if (!isExist) plugin._writeAllConfig(_file, ext, "{}");
            JsonObject base = Json.parse(plugin._readAllConfig(_file, ext)).getAsJsonObject();
            JsonElement data = base.has(parent) ? base.get(parent) : null;
            if (type == Type.Json) {
                if (data == null) {
                    base.add(parent, data = Json.parse(getDefault()));
                    plugin._writeAllConfig(_file, ext, Json.format(base));
                }
                invokeRead(data.toString());
            } else {
                if (data == null) {
                    base.add(parent, data = new JsonPrimitive(getDefault()));
                    plugin._writeAllConfig(_file, ext, Json.format(base));
                }
                invokeRead(data.getAsString());
            }
        }
    }
}
