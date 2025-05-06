package org.lime.core.common.api;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.core.common.json.builder.Json;
import org.lime.core.common.system.tuple.Tuple;
import org.lime.core.common.system.tuple.Tuple2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface BaseCombineJson extends BaseLogger, BaseConfig {
    default JsonElement $combineJson(JsonElement first, JsonElement second, boolean arrayJoin) {
        if (second.isJsonNull()) return first;
        if (first.isJsonArray()) {
            JsonArray _first = first.getAsJsonArray();
            JsonArray _second = second.getAsJsonArray();
            if (arrayJoin)
                _second.forEach(_first::add);
            /*
            if (array_join) _second.forEach(_first::add);
            else {
                int length = Math.min(_first.size(), _second.size());
                for (int i = 0; i < length; i++) _first.set(i, _combineJson(_first.get(i), _second.get(i), array_join));
            }
            */
        }
        else if (first.isJsonObject()) {
            JsonObject _first = first.getAsJsonObject();
            second.getAsJsonObject().entrySet().forEach(kv -> {
                String key = kv.getKey();
                if (_first.has(key)) _first.add(key, $combineJson(_first.get(key), kv.getValue(), arrayJoin));
                else _first.add(key, kv.getValue());
            });
        }
        return first;
    }
    default JsonElement $combineJson(JsonElement first, JsonElement second) {
        return $combineJson(first, second, true);
    }
    default JsonObject $combineParent(JsonObject json) {
        return $combineParent(json, false, true);
    }
    default JsonObject $combineParent(JsonObject json, boolean category, boolean array_join) {
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
        if (json.has("DEFAULT_REPLACE_FILE")) {
            json.get("DEFAULT_REPLACE_FILE").getAsJsonObject().entrySet().forEach(kv -> {
                String file = kv.getValue().getAsString();
                List<String> dat = new ArrayList<>(Arrays.asList(file.split("\\.")));
                if (dat.size() == 1) dat.add("json");
                replaceList.add(Tuple.of(kv.getKey(), $readAllConfig(dat.get(0), "." + dat.get(1))));
            });
            json.remove("DEFAULT_REPLACE_FILE");
        }
        json = Json.modifyObjectByKey(json, (key, value, obj) -> {
            if (key.isEmpty() || !value.isJsonPrimitive() || !key.replace("_", "").isEmpty())
                return false;
            JsonElement replaceJson = replaceJsonList.getOrDefault(value.getAsString(), null);
            if (replaceJson == null)
                return false;
            $combineJson(obj, replaceJson);
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
        this.$writeAllConfig("tmp.parent", Json.format(json));
        json.entrySet().forEach(kv -> {
            if (category) {
                kv.getValue().getAsJsonObject().entrySet().forEach(_kv -> {
                    JsonElement _obj = $combineParentElement(parentObjects, _kv.getKey(), _kv.getValue(), array_join);
                    if (_obj == null) return;
                    _new.add(_kv.getKey(), _obj);
                });
            } else {
                JsonElement _obj = $combineParentElement(parentObjects, kv.getKey(), kv.getValue(), array_join);
                if (_obj == null) return;
                _new.add(kv.getKey(), _obj);
            }
        });
        return _new;
    }

    private JsonElement $combineParentElement(HashMap<String, JsonObject> parentObjects, String key, JsonElement item, boolean array_join) {
        if (item.isJsonArray()) {
            JsonArray arr = item.getAsJsonArray();
            int length = arr.size();
            for (int i = 0; i < length; i++) {
                JsonElement element = $combineParentElement(parentObjects, key + "["+i+"]", arr.get(i), array_join);
                if (element == null) continue;
                arr.set(i, element);
            }
            return arr;
        }
        if (item.isJsonObject()) return $combineParentItem(parentObjects, key, item.getAsJsonObject(), array_join);
        return item;
    }
    private JsonObject $combineParentItem(HashMap<String, JsonObject> parentObjects, String key, JsonObject item, boolean array_join) {
        String parent = item.has("parent") ? item.get("parent").getAsString() : null;
        if (parent != null)
        {
            JsonObject _parent = parentObjects.getOrDefault(parent, null);
            if (_parent == null)
            {
                $logOP("[ERROR] PARENT '" + parent + "' NOT FOUNDED!");
                return null;
            }
            item = $combineJson(item, _parent, array_join).getAsJsonObject();
        }
        item.remove("parent");
        item.entrySet().forEach(sub -> {
            if (!sub.getValue().isJsonObject()) {
                JsonElement subElement = $combineParentElement(parentObjects, key + "." + sub.getKey(), sub.getValue(), array_join);
                if (subElement == null) return;
                sub.setValue(subElement);
                return;
            }
            JsonObject subItem = $combineParentItem(parentObjects, key + "." + sub.getKey(), sub.getValue().getAsJsonObject(), array_join);
            if (subItem == null) return;
            sub.setValue(subItem);
        });
        parentObjects.put(key, item);
        if (key.startsWith("_")) return null;

        return item;
    }
}
