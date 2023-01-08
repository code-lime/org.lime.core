package org.lime.json;

import com.google.gson.JsonNull;

public class JsonNullOptional extends JsonElementOptional {
    public static final JsonNullOptional INSTANCE = new JsonNullOptional();
    private JsonNullOptional() {}

    @Override public JsonNullOptional deepCopy() { return INSTANCE; }
    @Override public JsonNull base() { return JsonNull.INSTANCE; }

    public boolean equals(Object other) { return this == other || other instanceof JsonNull; }

    public static JsonNullOptional of() {
        return INSTANCE;
    }
}
