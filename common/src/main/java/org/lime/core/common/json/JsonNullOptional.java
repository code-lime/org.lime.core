package org.lime.core.common.json;

import com.google.gson.JsonNull;

public class JsonNullOptional extends JsonElementOptional {
    public static final JsonNullOptional INSTANCE = new JsonNullOptional();
    private JsonNullOptional() {}

    @Override public JsonNullOptional deepCopy() { return INSTANCE; }
    @Override public JsonNull base() { return JsonNull.INSTANCE; }

    @Override public Object createObject() { return null; }

    public boolean equals(Object other) { return this == other || other instanceof JsonNull; }

    public static JsonNullOptional of() {
        return INSTANCE;
    }
}
