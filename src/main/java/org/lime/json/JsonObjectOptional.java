package org.lime.json;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class JsonObjectOptional extends JsonElementOptional implements Map<String, JsonElementOptional> {
    private final LinkedTreeMap<String, JsonElementOptional> members = new LinkedTreeMap<>();

    @Override public JsonObjectOptional deepCopy() {
        JsonObjectOptional result = new JsonObjectOptional();
        this.forEach((key, value) -> result.add(key, value.deepCopy()));
        return result;
    }
    @Override public JsonObject base() {
        JsonObject json = new JsonObject();
        this.forEach((key, value) -> json.add(key, value.base()));
        return json;
    }

    public void add(String property, JsonElementOptional value) { this.members.put(property, value == null ? JsonNullOptional.INSTANCE : value); }
    public JsonElementOptional remove(String property) { return (JsonElementOptional)this.members.remove(property); }

    public void addProperty(String property, String value) { this.add(property, value == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(value)); }
    public void addProperty(String property, Number value) { this.add(property, value == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(value)); }
    public void addProperty(String property, Boolean value) { this.add(property, value == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(value)); }
    public void addProperty(String property, Character value) { this.add(property, value == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(value)); }

    @Override public Set<Map.Entry<String, JsonElementOptional>> entrySet() { return this.members.entrySet(); }
    @Override public int size() { return this.members.size(); }
    @Override public boolean isEmpty() { return this.members.isEmpty(); }
    @Override public boolean containsKey(Object key) { return this.members.containsKey(key); }
    @Override public boolean containsValue(Object value) { return this.members.containsValue(value); }
    @Override public JsonElementOptional get(Object key) { return this.members.get(key); }
    @Override public JsonElementOptional put(String key, JsonElementOptional value) { return this.members.put(key, value == null ? JsonNullOptional.INSTANCE : value); }
    @Override public JsonElementOptional remove(Object key) { return this.members.remove(key); }
    @Override public void putAll(Map<? extends String, ? extends JsonElementOptional> map) { map.forEach(this::add); }
    @Override public void clear() { this.members.clear(); }
    @Override public Set<String> keySet() { return this.members.keySet(); }
    @Override public Collection<JsonElementOptional> values() { return this.members.values(); }

    public boolean has(String memberName) { return this.members.containsKey(memberName); }

    public Optional<JsonElementOptional> get(String memberName) { return Optional.ofNullable(this.members.get(memberName)); }

    public Optional<JsonPrimitiveOptional> getAsJsonPrimitive(String memberName) {
        return Optional.ofNullable(this.members.get(memberName))
            .map(v -> v instanceof JsonPrimitiveOptional json ? json : null);
    }
    public Optional<JsonArrayOptional> getAsJsonArray(String memberName) {
        return Optional.ofNullable(this.members.get(memberName))
            .map(v -> v instanceof JsonArrayOptional json ? json : null);
    }
    public Optional<JsonObjectOptional> getAsJsonObject(String memberName) {
        return Optional.ofNullable(this.members.get(memberName))
            .map(v -> v instanceof JsonObjectOptional json ? json : null);
    }

    public Optional<Boolean> getAsBoolean(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsBoolean); }
    public Optional<Number> getAsNumber(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsNumber); }
    public Optional<String> getAsString(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsString); }
    public Optional<Double> getAsDouble(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsDouble); }
    public Optional<Float> getAsFloat(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsFloat); }
    public <T extends java.lang.Enum<T>>Optional<T> getAsEnum(Class<T> tClass, String memberName) { return get(memberName).flatMap(v -> v.getAsEnum(tClass)); }
    public Optional<BigDecimal> getAsBigDecimal(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsBigDecimal); }
    public Optional<BigInteger> getAsBigInteger(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsBigInteger); }
    public Optional<Long> getAsLong(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsLong); }
    public Optional<Short> getAsShort(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsShort); }
    public Optional<Integer> getAsInt(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsInt); }
    public Optional<Byte> getAsByte(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsByte); }
    public Optional<Character> getAsCharacter(String memberName) { return get(memberName).flatMap(JsonElementOptional::getAsCharacter); }

    @Override public Map<String, Object> createObject() { return this.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey(), kv -> kv.getValue().createObject())); }

    public boolean equals(Object o) { return o == this || o instanceof JsonObjectOptional json && json.members.equals(this.members); }
    public int hashCode() { return this.members.hashCode(); }

    public static JsonObjectOptional of(JsonObject json) {
        JsonObjectOptional result = new JsonObjectOptional();
        json.entrySet().forEach(kv -> result.add(kv.getKey(), JsonElementOptional.of(kv.getValue())));
        return result;
    }
}












