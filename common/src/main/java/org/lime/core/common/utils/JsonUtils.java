package org.lime.core.common.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.StringWriter;
import java.util.Optional;

public class JsonUtils {
    public static Optional<JsonElement> merge(JsonElement current, JsonElement target) {
        JsonElement result = current.deepCopy();
        return mergeTo(result, target) ? Optional.of(result) : Optional.empty();
    }
    public static Optional<JsonObject> merge(JsonObject current, JsonObject target) {
        JsonObject result = current.deepCopy();
        return mergeTo(result, target) ? Optional.of(result) : Optional.empty();
    }

    private static boolean mergeTo(JsonObject current, JsonObject target) {
        boolean merged = false;
        for (var entry : target.entrySet()) {
            String key = entry.getKey();
            JsonElement targetValue = entry.getValue();
            JsonElement currentValue = current.get(key);
            if (currentValue == null) {
                current.add(key, targetValue.deepCopy());
                merged = true;
            } else {
                merged |= mergeTo(currentValue, targetValue);
            }
        }
        return merged;
    }
    private static boolean mergeTo(JsonElement current, JsonElement target) {
        if (current instanceof JsonObject currentObject && target instanceof JsonObject targetObject)
            return mergeTo(currentObject, targetObject);
        return false;
    }

    public static String toJson(Gson gson, JsonElement element) {
        StringWriter writer = new StringWriter();
        gson.toJson(element, writer);
        return writer.toString();
    }
}
