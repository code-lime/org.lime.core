package org.lime.core.common.api.elements;

import com.google.gson.*;
import org.lime.core.common.api.BaseConfig;
import org.lime.core.common.system.execute.Action1;
import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.json.builder.Json;

import java.util.ArrayList;
import java.util.List;

public class CoreResource<T> {
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
    public final CoreResource<?> ifEmpty;
    public final Action1<T> invoke;
    public final Func0<T> def;

    public final Func1<String, T> read;
    public final Func1<T, String> write;

    private CoreResource(Type type, String file, String name, String parent, Action1<T> invoke, Func0<T> def, Func1<String, T> read, Func1<T, String> write, CoreResource<?> ifEmpty) {
        this.type = type;
        this.file = file;
        this.name = name;
        this.parent = parent;
        this.invoke = invoke;
        this.def = def;
        this.ifEmpty = ifEmpty;

        this.read = read;
        this.write = write;
    }

    private static <T> CoreResource<T> create(Type type, Func1<String, T> read, Func1<T, String> write) {
        return new CoreResource<>(type, null, null, null, null, null, read, write, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> CoreResource<T> json() {
        return create(Type.Json, text -> (T) Json.parse(text), Json::format);
    }

    public static <T> CoreResource<T> gson(Class<T> rawClass, Func1<GsonBuilder, GsonBuilder> configure) {
        GsonBuilder builder = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .serializeNulls()
                .disableHtmlEscaping();
        builder = configure.invoke(builder);
        Gson gson = builder.create();
        return create(Type.Json,
                v -> gson.fromJson(v, rawClass),
                v -> Json.format(gson.toJsonTree(v, rawClass)));
    }

    public static CoreResource<String> text() {
        return create(Type.Text, v -> v, v -> v);
    }

    static CoreResource<Object> none() {
        return create(Type.None, null, null);
    }

    static CoreResource<Object> init() {
        return create(Type.Init, null, null);
    }

    public CoreResource<T> withInvoke(Action1<T> invoke) {
        return new CoreResource<>(type, file, name, parent, invoke, def, read, write, ifEmpty);
    }

    public CoreResource<T> withDefault(Func0<T> def) {
        return new CoreResource<>(type, file, name, parent, invoke, def, read, write, ifEmpty);
    }

    public CoreResource<T> withDefault(T def) {
        return withDefault(() -> def);
    }

    public CoreResource<T> withParent(String parent) {
        return new CoreResource<>(type, file, name, parent, invoke, def, read, write, ifEmpty);
    }


    public CoreResource<T> orText(String file, Func1<CoreResource<String>, CoreResource<String>> builder) {
        return orFile(file, file, builder.invoke(CoreResource.text()));
    }

    public <T2 extends JsonElement> CoreResource<T> orConfig(String config, Func1<CoreResource<T2>, CoreResource<T2>> builder) {
        return orFile(config + ".json", config, builder.invoke(CoreResource.json()));
    }

    public CoreResource<T> orFile(String file, String name, CoreResource<?> data) {
        return new CoreResource<>(type, this.file, this.name, parent, invoke, def, read, write, data.withFile(file).withName(name));
    }

    CoreResource<T> withFile(String file) { return new CoreResource<>(type, file, name, parent, invoke, def, read, write, ifEmpty); }
    CoreResource<T> withName(String name) { return new CoreResource<>(type, file, name, parent, invoke, def, read, write, ifEmpty); }

    private void invokeRead(String text) {
        invoke.invoke(read.invoke(text));
    }

    private String getDefault() {
        return def == null ? "" : write.invoke(def.invoke());
    }

    public List<String> getFiles() {
        List<String> list = new ArrayList<>();
        list.add(name == null ? file : name);
        if (ifEmpty != null) list.addAll(ifEmpty.getFiles());
        return list;
    }

    public void read(BaseConfig config, boolean update) {
        if (read == null || write == null) {
            if (update || type.init) invoke.invoke(null);
            return;
        }
        String[] split = file.split("\\.");
        String _file = split[0];
        String ext = "." + split[1];

        boolean isExist = config.$existConfig(_file, ext);

        if (!isExist && ifEmpty != null) {
            ifEmpty.read(config, update);
            return;
        }

        if (parent == null) {
            if (!isExist) config.$writeAllConfig(_file, ext, getDefault());
            invokeRead(config.$readAllConfig(_file, ext));
        } else {
            if (!isExist) config.$writeAllConfig(_file, ext, "{}");
            JsonObject base = Json.parse(config.$readAllConfig(_file, ext)).getAsJsonObject();
            JsonElement data = base.has(parent) ? base.get(parent) : null;
            if (type == Type.Json) {
                if (data == null) {
                    base.add(parent, data = Json.parse(getDefault()));
                    config.$writeAllConfig(_file, ext, Json.format(base));
                }
                invokeRead(data.toString());
            } else {
                if (data == null) {
                    base.add(parent, data = new JsonPrimitive(getDefault()));
                    config.$writeAllConfig(_file, ext, Json.format(base));
                }
                invokeRead(data.getAsString());
            }
        }
    }
}
