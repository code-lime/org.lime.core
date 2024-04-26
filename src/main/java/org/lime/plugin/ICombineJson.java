package org.lime.plugin;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.JavaScript;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;
import org.lime.system.toast.Toast2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface ICombineJson extends ILogger, IConfig {
    default JavaScript js() { throw new IllegalAccessError("JavaScript module not overrided"); }

    default JsonElement _combineJson(JsonElement first, JsonElement second, boolean array_join) {
        if (second.isJsonNull()) return first;
        if (first.isJsonArray()) {
            JsonArray _first = first.getAsJsonArray();
            JsonArray _second = second.getAsJsonArray();
            if (array_join) _second.forEach(_first::add);
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
                if (_first.has(key)) _first.add(key, _combineJson(_first.get(key), kv.getValue(), array_join));
                else _first.add(key, kv.getValue());
            });
        }
        return first;
    }
    default JsonElement _combineJson(JsonElement first, JsonElement second) {
        return _combineJson(first, second, true);
    }
    default JsonObject _combineParent(JsonObject json) {
        return _combineParent(json, false, true);
    }
    default JsonObject _combineParent(JsonObject _json, boolean category, boolean array_join) {
        HashMap<String, JsonObject> parentObjects = new HashMap<>();
        JsonObject _new = new JsonObject();
        List<Toast2<String, String>> replaceList = new ArrayList<>();
        HashMap<String, JsonElement> replaceJsonList = new HashMap<>();
        if (_json.has("DEFAULT_REPLACE")) {
            _json.get("DEFAULT_REPLACE").getAsJsonObject().entrySet().forEach(kv -> {
                if (kv.getValue().isJsonArray()) replaceList.add(Toast.of(kv.getKey(), Streams.stream(kv.getValue().getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.joining("\\n"))));
                else replaceList.add(Toast.of(kv.getKey(), kv.getValue().getAsString()));
            });
            _json.remove("DEFAULT_REPLACE");
        }
        if (_json.has("DEFAULT_REPLACE_JSON")) {
            _json.get("DEFAULT_REPLACE_JSON").getAsJsonObject().entrySet().forEach(kv -> {
                replaceJsonList.put(kv.getKey(), json.deepCopy(kv.getValue()));
            });
            _json.remove("DEFAULT_REPLACE_JSON");
        }
        if (_json.has("DEFAULT_REPLACE_FILE")) {
            _json.get("DEFAULT_REPLACE_FILE").getAsJsonObject().entrySet().forEach(kv -> {
                String file = kv.getValue().getAsString();
                List<String> dat = new ArrayList<>(Arrays.asList(file.split("\\.")));
                if (dat.size() == 1) dat.add("json");
                replaceList.add(Toast.of(kv.getKey(), _readAllConfig(dat.get(0), "." + dat.get(1))));
            });
            _json.remove("DEFAULT_REPLACE_FILE");
        }
        _json = org.lime.system.json.modifyObjectByKey(_json, (key, value, obj) -> {
            if (key.isEmpty() || !value.isJsonPrimitive() || !key.replace("_", "").isEmpty())
                return false;
            JsonElement replaceJson = replaceJsonList.getOrDefault(value.getAsString(), null);
            if (replaceJson == null)
                return false;
            _combineJson(obj, replaceJson);
            return true;
        });
        _json = org.lime.system.json.editStringToObject(_json, text -> {
            JsonElement replaceJson = replaceJsonList.getOrDefault(text, null);
            return replaceJson == null ? new JsonPrimitive(text) : replaceJson;
        });
        _json = org.lime.system.json.editStringToObject(_json, text -> {
            for (Toast2<String, String> replace : replaceList)
                text = text.replaceAll(
                        Pattern.quote("{" + replace.val0 + "}"),
                        Matcher.quoteReplacement(replace.val1));
            return new JsonPrimitive(text);
        });
        _json = _executeJS(_json);
        this._writeAllConfig("tmp.parent", org.lime.system.json.format(_json));
        _json.entrySet().forEach(kv -> {
            if (category) {
                kv.getValue().getAsJsonObject().entrySet().forEach(_kv -> {
                    JsonElement _obj = _combineParentElement(parentObjects, _kv.getKey(), _kv.getValue(), array_join);
                    if (_obj == null) return;
                    _new.add(_kv.getKey(), _obj);
                });
            } else {
                JsonElement _obj = _combineParentElement(parentObjects, kv.getKey(), kv.getValue(), array_join);
                if (_obj == null) return;
                _new.add(kv.getKey(), _obj);
            }
        });
        return _new;
    }

    private JsonElement _combineParentElement(HashMap<String, JsonObject> parentObjects, String key, JsonElement item, boolean array_join) {
        if (item.isJsonArray()) {
            JsonArray arr = item.getAsJsonArray();
            int length = arr.size();
            for (int i = 0; i < length; i++) {
                JsonElement element = _combineParentElement(parentObjects, key + "["+i+"]", arr.get(i), array_join);
                if (element == null) continue;
                arr.set(i, element);
            }
            return arr;
        }
        if (item.isJsonObject()) return _combineParentItem(parentObjects, key, item.getAsJsonObject(), array_join);
        return item;
    }
    private JsonObject _combineParentItem(HashMap<String, JsonObject> parentObjects, String key, JsonObject item, boolean array_join) {
        String parent = item.has("parent") ? item.get("parent").getAsString() : null;
        if (parent != null)
        {
            JsonObject _parent = parentObjects.getOrDefault(parent, null);
            if (_parent == null)
            {
                _logOP("[ERROR] PARENT '" + parent + "' NOT FOUNDED!");
                return null;
            }
            item = _combineJson(item, _parent, array_join).getAsJsonObject();
        }
        item.remove("parent");
        item.entrySet().forEach(sub -> {
            if (!sub.getValue().isJsonObject()) {
                JsonElement sub_element = _combineParentElement(parentObjects, key + "." + sub.getKey(), sub.getValue(), array_join);
                if (sub_element == null) return;
                sub.setValue(sub_element);
                return;
            }
            JsonObject sub_item = _combineParentItem(parentObjects, key + "." + sub.getKey(), sub.getValue().getAsJsonObject(), array_join);
            if (sub_item == null) return;
            sub.setValue(sub_item);
        });
        parentObjects.put(key, item);
        if (key.startsWith("_")) return null;

        return item;
    }

    private Optional<JsonElement> _executePartOfJS(JavaScript js, JsonElement element) {
        return _executePartOfJS(js, element, Collections.emptyMap());
    }
    private Optional<JsonElement> _executePartOfJS(JavaScript js, JsonElement element, Map<String, Object> setup) {
        if (element.isJsonObject()) {
            JsonObjectOptional json = JsonObjectOptional.of(element.getAsJsonObject());
            String code = json.getAsString("code").orElseThrow();
            HashMap<String, Object> data = new HashMap<>(setup);
            json.getAsJsonObject("args")
                    .ifPresent(args -> args
                            .forEach((key, value) -> data.put(key, value.createObject())));
            return js.getJsJson(code, data);
        }
        return js.getJsJson(element.getAsString());
    }
    private JsonObject _executeJS(JsonObject _json) {
        if (_json.has("GENERATE_JS_APPEND")) {
            JavaScript js = js();
            Toast1<JsonObject> append = Toast.of(new JsonObject());
            _json.getAsJsonArray("GENERATE_JS_APPEND").forEach(_js -> {
                if (_js.isJsonPrimitive())
                    _executePartOfJS(js, _js)
                            .ifPresent(result -> append.val0 = _combineJson(append.val0, result, false).getAsJsonObject());
                else if (_js.isJsonArray())
                    _js.getAsJsonArray().forEach(__js -> {
                        _executePartOfJS(js, _js)
                                .ifPresent(result -> append.val0 = _combineJson(append.val0, result, false).getAsJsonObject());
                    });
                else if (_js.isJsonObject())
                    _js.getAsJsonObject().entrySet().forEach(_kv -> {
                        if (_kv.getKey().startsWith("!"))
                            _executePartOfJS(js, _kv.getValue(), Collections.singletonMap("key", _kv.getKey().substring(1)))
                                    .ifPresent(result -> append.val0 = _combineJson(append.val0, result, false).getAsJsonObject());
                        else
                            _executePartOfJS(js, _kv.getValue(), Collections.singletonMap("key", _kv.getKey()))
                                    .ifPresent(result -> append.val0.add(_kv.getKey(), append.val0.has(_kv.getKey())
                                            ? _combineJson(append.val0, append.val0.get(_kv.getKey()), false)
                                            : result));
                    });
            });
            _json = _combineJson(json.deepCopy(_json), append.val0, false).getAsJsonObject();
            _json.remove("GENERATE_JS_APPEND");
        }
        _json.entrySet().forEach(kv -> {
            if (!kv.getValue().isJsonObject()) return;
            kv.setValue(_executeJS(kv.getValue().getAsJsonObject()));
        });
        return _json;
    }
}
