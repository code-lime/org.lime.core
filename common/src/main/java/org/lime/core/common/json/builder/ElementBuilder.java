package org.lime.core.common.json.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElementBuilder extends BaseBuilder<JsonElement> {
    private final JsonElement json;

    private ElementBuilder(JsonElement json) {
        this.json = json;
    }

    private ElementBuilder() {
        this(JsonNull.INSTANCE);
    }

    private ElementBuilder(String value) {
        this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private ElementBuilder(Number value) {
        this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private ElementBuilder(Boolean value) {
        this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private ElementBuilder(Character value) {
        this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public static ElementBuilder create() {
        return new ElementBuilder();
    }

    public static ElementBuilder create(JsonElement value) {
        return new ElementBuilder(value);
    }

    public static ElementBuilder create(BaseBuilder<?> value) {
        return new ElementBuilder(value.build());
    }

    public static ElementBuilder create(String value) {
        return new ElementBuilder(value);
    }

    public static ElementBuilder create(Number value) {
        return new ElementBuilder(value);
    }

    public static ElementBuilder create(Boolean value) {
        return new ElementBuilder(value);
    }

    public static ElementBuilder create(Character value) {
        return new ElementBuilder(value);
    }

    @Override
    public JsonElement build() {
        return json;
    }
}
