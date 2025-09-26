package org.lime.core.common.utils;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;

import java.util.*;
import java.util.stream.Stream;

public class ParseUtils {
    public static <T>void parseAdd(JsonObject json, String key, Collection<T> list, Collection<T> data, Func1<T, String> name) {
        parseList(json, key, addByList(list, data, name));
    }
    public static <T>List<T> parseGet(JsonObject json, String key, Collection<T> list, Func1<T, String> name) {
        List<T> data = new ArrayList<>();
        parseAdd(json, key, data, list, name);
        return data;
    }
    public static void parseList(JsonObject json, String key, Action2<JsonArray, Boolean> callback) {
        if (json.has(key)) callback.invoke(json.get(key).getAsJsonArray(), true);
        else if (json.has("!" + key)) callback.invoke(json.get("!" + key).getAsJsonArray(), false);
    }
    private static Stream<String> getStringsInArrayDeep(JsonArray array) {
        return Streams.stream(array.iterator()).flatMap(v -> v.isJsonObject()
                ? v.getAsJsonObject()
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(JsonElement::getAsJsonArray)
                .flatMap(ParseUtils::getStringsInArrayDeep)
                : v.isJsonArray()
                ? getStringsInArrayDeep(v.getAsJsonArray())
                : Stream.of(v.getAsString()));
    }
    public static <T> Action2<JsonArray, Boolean> addByList(Collection<T> list, Collection<T> data, Func1<T, String> name) {
        return (json, add_to_empty) -> {
            if (add_to_empty) {
                HashMap<T, Boolean> _list = new HashMap<>();
                getStringsInArrayDeep(json).forEach(item -> {
                    for (T material : data) {
                        if (RegexUtils.compareRegex(name.invoke(material), item)) {
                            _list.put(material, true);
                        }
                    }
                });
                list.addAll(_list.keySet());
            } else {
                List<T> _list = new ArrayList<>(data);
                _list.removeIf(material -> {
                    for (String item : getStringsInArrayDeep(json).toList()) {
                        if (RegexUtils.compareRegex(name.invoke(material), item))
                            return true;
                    }
                    return false;
                });
                list.addAll(_list);
            }
        };
    }
}
