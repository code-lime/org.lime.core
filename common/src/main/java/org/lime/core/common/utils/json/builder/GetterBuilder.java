package org.lime.core.common.utils.json.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.tuple.Tuple;
import org.lime.core.common.utils.tuple.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetterBuilder extends BaseBuilder<JsonElement> {
    private final List<Tuple2<String, Func1<JsonElement, JsonElement>>> list = new ArrayList<>();
    private final JsonElement base;

    GetterBuilder(JsonElement base) {
        this.base = base;
    }

    public GetterBuilder of(int index) {
        list.add(Tuple.of("[" + index + "]", json -> json != null && json.isJsonArray() ? json.getAsJsonArray().get(index) : null));
        return this;
    }

    public GetterBuilder last() {
        list.add(Tuple.of("[last]", json -> {
            if (!json.isJsonArray()) return null;
            JsonArray arr = json.getAsJsonArray();
            int length = arr.size();
            return length == 0 ? null : arr.get(length - 1);
        }));
        return this;
    }

    public GetterBuilder of(String key) {
        list.add(Tuple.of("." + key, json -> json != null && json.isJsonObject() ? json.getAsJsonObject().get(key) : null));
        return this;
    }

    @Override
    public JsonElement build() {
        JsonElement json = base;
        for (Tuple2<String, Func1<JsonElement, JsonElement>> item : list)
            json = item.val1.invoke(json);
        return json;
    }

    public JsonArray array() {
        JsonElement json = build();
        return json == null || !json.isJsonArray() ? null : json.getAsJsonArray();
    }

    public JsonObject object() {
        JsonElement json = build();
        return json == null || !json.isJsonObject() ? null : json.getAsJsonObject();
    }

    public JsonPrimitive primitive() {
        JsonElement json = build();
        return json == null || !json.isJsonPrimitive() ? null : json.getAsJsonPrimitive();
    }

    public <T> T other(Func1<JsonElement, T> parse, T def) {
        return otherFunc(parse, () -> def);
    }

    public <T> T other(Func1<JsonElement, T> parse) {
        return otherFunc(parse, () -> {
            throw new IllegalArgumentException("Path 'JSON." + list.stream().map(v -> v.val0).collect(Collectors.joining()) + "' not founded!");
        });
    }

    public <T> T otherFunc(Func1<JsonElement, T> parse, Func0<T> def) {
        JsonElement json = build();
        if (json == null) return def.invoke();
        try {
            return parse.invoke(json);
        } catch (Exception e) {
            return def.invoke();
        }
    }
}
