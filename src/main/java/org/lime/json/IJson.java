package org.lime.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.system.execute.Func1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IJson<T extends JsonElement> {
    abstract class ILoad<T extends JsonElement> {
        protected ILoad(T json) {
        }

        public static <T extends JsonElement, I extends ILoad<T>> I parse(Func1<T, I> parse, T json) {
            return parse.invoke(json);
        }

        public static <T extends JsonElement, I extends ILoad<T>> List<I> parse(Func1<T, I> parse, JsonArray json) {
            List<I> list = new ArrayList<>();
            json.forEach(item -> list.add(parse.invoke((T) item)));
            return list;
        }

        public static <T extends JsonElement, I extends ILoad<T>> HashMap<String, I> parse(Func1<T, I> parse, JsonObject json) {
            return parse(parse, json, v -> v);
        }

        public static <TKey, T extends JsonElement, I extends ILoad<T>> HashMap<TKey, I> parse(Func1<T, I> parse, JsonObject json, Func1<String, TKey> key) {
            HashMap<TKey, I> list = new HashMap<>();
            json.entrySet().forEach(kv -> list.put(key.invoke(kv.getKey()), parse.invoke((T) kv.getValue())));
            return list;
        }
    }

    T toJson();

    static <TValue extends IJson<?>> JsonObject toJson(Map<String, TValue> map) {
        return toJson(map, v -> v);
    }

    static <TKey, TValue extends IJson<?>> JsonObject toJson(Map<TKey, TValue> map, Func1<TKey, String> key) {
        JsonObject json = new JsonObject();
        map.forEach((k, v) -> json.add(key.invoke(k), v.toJson()));
        return json;
    }

    static <TValue extends IJson<?>> JsonArray toJson(List<TValue> list) {
        JsonArray json = new JsonArray();
        list.forEach(v -> json.add(v.toJson()));
        return json;
    }
}
