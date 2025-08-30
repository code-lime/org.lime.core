package org.lime.core.common.utils.json.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.core.common.utils.system.execute.Func1;

import java.util.Iterator;
import java.util.Map;

public class ObjectBuilder extends BaseBuilder<JsonObject> {
    private final JsonObject json;

    ObjectBuilder(JsonObject json) {
        this.json = json;
    }

    ObjectBuilder() {
        this(new JsonObject());
    }

    public ObjectBuilder addObject(String key, Func1<ObjectBuilder, ObjectBuilder> value) {
        return add(key, value.invoke(new ObjectBuilder()));
    }

    public ObjectBuilder addArray(String key, Func1<ArrayBuilder, ArrayBuilder> value) {
        return add(key, value.invoke(new ArrayBuilder()));
    }

    public <TValue> ObjectBuilder add(Map<String, TValue> map) {
        map.forEach(this::add);
        return this;
    }

    public <TKey, TValue> ObjectBuilder add(Map<TKey, TValue> map, Func1<TKey, String> key, Func1<TValue, Object> value) {
        map.forEach((k, v) -> add(key.invoke(k), value.invoke(v)));
        return this;
    }

    public <T> ObjectBuilder add(Iterable<T> list, Func1<T, String> key, Func1<T, Object> value) {
        list.forEach(i -> add(key.invoke(i), value.invoke(i)));
        return this;
    }

    public <T> ObjectBuilder add(Iterator<T> list, Func1<T, String> key, Func1<T, Object> value) {
        list.forEachRemaining(i -> add(key.invoke(i), value.invoke(i)));
        return this;
    }

    public ObjectBuilder add(String key, JsonElement value) {
        json.add(key, value);
        return this;
    }

    public ObjectBuilder add(JsonObject json) {
        json.entrySet().forEach(kv -> this.add(kv.getKey(), kv.getValue()));
        return this;
    }

    public ObjectBuilder addNull(String key) {
        return add(key, byObject(null));
    }

    public ObjectBuilder add(String key, BaseBuilder<?> value) {
        return value == null ? addNull(key) : add(key, value.build());
    }

    public ObjectBuilder add(String key, Object value) {
        return add(key, byObject(value));
    }

    public ObjectBuilder add(ObjectBuilder json) {
        return add(json.build());
    }

    @Override
    public JsonObject build() {
        return json;
    }
}
