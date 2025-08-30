package org.lime.core.common.services;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import org.lime.core.common.utils.json.builder.Json;
import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple2;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonCombineUtility {
    @Inject Logger logger;

    public JsonElement combineJson(JsonElement first, JsonElement second, boolean arrayJoin) {
        if (second.isJsonNull()) return first;
        if (first.isJsonArray()) {
            if (arrayJoin)
                second.getAsJsonArray().forEach(first.getAsJsonArray()::add);
        }
        else if (first.isJsonObject()) {
            JsonObject firstObject = first.getAsJsonObject();
            second.getAsJsonObject().entrySet().forEach(kv -> {
                String key = kv.getKey();
                if (firstObject.has(key)) firstObject.add(key, combineJson(firstObject.get(key), kv.getValue(), arrayJoin));
                else firstObject.add(key, kv.getValue());
            });
        }
        return first;
    }
    public JsonElement combineJson(JsonElement first, JsonElement second) {
        return combineJson(first, second, true);
    }
    public JsonObject combineParent(JsonObject json) {
        return combineParent(json, false, true);
    }
    public JsonObject combineParent(JsonObject json, boolean category, boolean arrayJoin) {
        HashMap<String, JsonObject> parentObjects = new HashMap<>();
        JsonObject _new = new JsonObject();
        List<Tuple2<String, String>> replaceList = new ArrayList<>();
        HashMap<String, JsonElement> replaceJsonList = new HashMap<>();
        if (json.has("DEFAULT_REPLACE")) {
            json.get("DEFAULT_REPLACE").getAsJsonObject().entrySet().forEach(kv -> {
                if (kv.getValue().isJsonArray()) replaceList.add(Tuple.of(kv.getKey(), Streams.stream(kv.getValue().getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.joining("\\n"))));
                else replaceList.add(Tuple.of(kv.getKey(), kv.getValue().getAsString()));
            });
            json.remove("DEFAULT_REPLACE");
        }
        if (json.has("DEFAULT_REPLACE_JSON")) {
            json.get("DEFAULT_REPLACE_JSON").getAsJsonObject().entrySet().forEach(kv -> {
                replaceJsonList.put(kv.getKey(), Json.deepCopy(kv.getValue()));
            });
            json.remove("DEFAULT_REPLACE_JSON");
        }
        json = Json.modifyObjectByKey(json, (key, value, obj) -> {
            if (key.isEmpty() || !value.isJsonPrimitive() || !key.replace("_", "").isEmpty())
                return false;
            JsonElement replaceJson = replaceJsonList.getOrDefault(value.getAsString(), null);
            if (replaceJson == null)
                return false;
            combineJson(obj, replaceJson);
            return true;
        });
        json = Json.editStringToObject(json, text -> {
            JsonElement replaceJson = replaceJsonList.getOrDefault(text, null);
            return replaceJson == null ? new JsonPrimitive(text) : replaceJson;
        });
        json = Json.editStringToObject(json, text -> {
            for (Tuple2<String, String> replace : replaceList)
                text = text.replaceAll(
                        Pattern.quote("{" + replace.val0 + "}"),
                        Matcher.quoteReplacement(replace.val1));
            return new JsonPrimitive(text);
        });
        json.entrySet().forEach(kv -> {
            if (category) {
                kv.getValue().getAsJsonObject().entrySet().forEach(_kv -> {
                    JsonElement _obj = combineParentElement(parentObjects, _kv.getKey(), _kv.getValue(), arrayJoin);
                    if (_obj == null) return;
                    _new.add(_kv.getKey(), _obj);
                });
            } else {
                JsonElement _obj = combineParentElement(parentObjects, kv.getKey(), kv.getValue(), arrayJoin);
                if (_obj == null) return;
                _new.add(kv.getKey(), _obj);
            }
        });
        return _new;
    }

    private JsonElement combineParentElement(HashMap<String, JsonObject> parentObjects, String key, JsonElement item, boolean  arrayJoin) {
        if (item.isJsonArray()) {
            JsonArray arr = item.getAsJsonArray();
            int length = arr.size();
            for (int i = 0; i < length; i++) {
                JsonElement element = combineParentElement(parentObjects, key + "["+i+"]", arr.get(i),  arrayJoin);
                if (element == null) continue;
                arr.set(i, element);
            }
            return arr;
        }
        if (item.isJsonObject()) return combineParentItem(parentObjects, key, item.getAsJsonObject(),  arrayJoin);
        return item;
    }
    private JsonObject combineParentItem(HashMap<String, JsonObject> parentObjects, String key, JsonObject item, boolean  arrayJoin) {
        String parent = item.has("parent") ? item.get("parent").getAsString() : null;
        if (parent != null)
        {
            JsonObject _parent = parentObjects.getOrDefault(parent, null);
            if (_parent == null)
            {
                logger.error("Parent '{}' not founded!", parent);
                return null;
            }
            item = combineJson(item, _parent,  arrayJoin).getAsJsonObject();
        }
        item.remove("parent");
        item.entrySet().forEach(sub -> {
            if (!sub.getValue().isJsonObject()) {
                JsonElement subElement = combineParentElement(parentObjects, key + "." + sub.getKey(), sub.getValue(),  arrayJoin);
                if (subElement == null) return;
                sub.setValue(subElement);
                return;
            }
            JsonObject subItem = combineParentItem(parentObjects, key + "." + sub.getKey(), sub.getValue().getAsJsonObject(),  arrayJoin);
            if (subItem == null) return;
            sub.setValue(subItem);
        });
        parentObjects.put(key, item);
        if (key.startsWith("_")) return null;

        return item;
    }
}
