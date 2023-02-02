package org.lime.json;

import com.google.gson.JsonArray;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class JsonArrayOptional extends JsonElementOptional implements Collection<JsonElementOptional> {
    private final List<JsonElementOptional> elements = new ArrayList<>();

    @Override public JsonArrayOptional deepCopy() {
        JsonArrayOptional result = new JsonArrayOptional();
        this.elements.forEach(element -> result.add(element.deepCopy()));
        return result;
    }
    @Override public JsonArray base() {
        JsonArray array = new JsonArray();
        this.elements.forEach(element -> array.add(element.base()));
        return array;
    }

    public void add(Boolean bool) { this.elements.add(bool == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(bool)); }
    public void add(Character character) { this.elements.add(character == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(character)); }
    public void add(Number number) { this.elements.add(number == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(number)); }
    public void add(String string) { this.elements.add(string == null ? JsonNullOptional.INSTANCE : new JsonPrimitiveOptional(string)); }
    public boolean add(JsonElementOptional element) { return this.elements.add(element == null ? JsonNullOptional.INSTANCE : element); }

    @Override public boolean remove(Object value) { return this.elements.remove(value); }
    @Override public boolean containsAll(Collection<?> collection) { return this.elements.containsAll(collection); }
    @Override public boolean addAll(Collection<? extends JsonElementOptional> collection) {
        boolean isAdded = false;
        for (JsonElementOptional item : collection) isAdded = add(item) || isAdded;
        return isAdded;
    }
    @Override public boolean removeAll(Collection<?> collection) { return this.elements.removeAll(collection); }
    @Override public boolean retainAll(Collection<?> collection) { return this.elements.retainAll(collection); }
    @Override public void clear() { this.elements.clear(); }

    public void addAll(JsonArrayOptional array) { this.elements.addAll(array.elements); }

    private boolean inRange(int index) { return index > 0 && index < this.elements.size(); }

    public JsonElementOptional set(int index, JsonElementOptional element) { return this.elements.set(index, element == null ? JsonNullOptional.INSTANCE : element); }
    public boolean remove(JsonElementOptional element) { return this.elements.remove(element); }
    public Optional<JsonElementOptional> remove(int index) { return inRange(index) ? Optional.of(this.elements.remove(index)) : Optional.empty(); }
    public boolean contains(JsonElementOptional element) { return this.elements.contains(element); }
    public int size() { return this.elements.size(); }

    @Override public boolean isEmpty() { return this.elements.isEmpty(); }
    @Override public boolean contains(Object value) { return this.elements.contains(value); }
    public Iterator<JsonElementOptional> iterator() { return this.elements.iterator(); }
    @Override public Object[] toArray() { return this.elements.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return this.elements.toArray(a); }

    public Optional<JsonElementOptional> get(int index) { return inRange(index) ? Optional.of(this.elements.get(index)) : Optional.empty(); }
    public Optional<Number> getAsNumber(int index) { return get(index).flatMap(JsonElementOptional::getAsNumber); }
    public Optional<String> getAsString(int index) { return get(index).flatMap(JsonElementOptional::getAsString); }
    public Optional<Double> getAsDouble(int index) { return get(index).flatMap(JsonElementOptional::getAsDouble); }
    public Optional<BigDecimal> getAsBigDecimal(int index) { return get(index).flatMap(JsonElementOptional::getAsBigDecimal); }
    public Optional<BigInteger> getAsBigInteger(int index) { return get(index).flatMap(JsonElementOptional::getAsBigInteger); }
    public Optional<Float> getAsFloat(int index) { return get(index).flatMap(JsonElementOptional::getAsFloat); }
    public Optional<Long> getAsLong(int index) { return get(index).flatMap(JsonElementOptional::getAsLong); }
    public Optional<Integer> getAsInt(int index) { return get(index).flatMap(JsonElementOptional::getAsInt); }
    public Optional<Byte> getAsByte(int index) { return get(index).flatMap(JsonElementOptional::getAsByte); }
    public Optional<Character> getAsCharacter(int index) { return get(index).flatMap(JsonElementOptional::getAsCharacter); }
    public Optional<Short> getAsShort(int index) { return get(index).flatMap(JsonElementOptional::getAsShort); }
    public Optional<Boolean> getAsBoolean(int index) { return get(index).flatMap(JsonElementOptional::getAsBoolean); }
    public Optional<JsonObjectOptional> getAsJsonObject(int index) { return get(index).flatMap(JsonElementOptional::getAsJsonObject); }
    public Optional<JsonArrayOptional> getAsJsonArray(int index) { return get(index).flatMap(JsonElementOptional::getAsJsonArray); }

    private Optional<JsonElementOptional> getAs() { return this.elements.size() == 1 ? Optional.of(this.elements.get(0)) : Optional.empty(); }
    public Optional<Object> getAsObject() { return getAs().flatMap(JsonElementOptional::getAsObject); }
    public Optional<Number> getAsNumber() { return getAs().flatMap(JsonElementOptional::getAsNumber); }
    public Optional<String> getAsString() { return getAs().flatMap(JsonElementOptional::getAsString); }
    public Optional<Double> getAsDouble() { return getAs().flatMap(JsonElementOptional::getAsDouble); }
    public Optional<BigDecimal> getAsBigDecimal() { return getAs().flatMap(JsonElementOptional::getAsBigDecimal); }
    public Optional<BigInteger> getAsBigInteger() { return getAs().flatMap(JsonElementOptional::getAsBigInteger); }
    public Optional<Float> getAsFloat() { return getAs().flatMap(JsonElementOptional::getAsFloat); }
    public Optional<Long> getAsLong() { return getAs().flatMap(JsonElementOptional::getAsLong); }
    public Optional<Integer> getAsInt() { return getAs().flatMap(JsonElementOptional::getAsInt); }
    public Optional<Byte> getAsByte() { return getAs().flatMap(JsonElementOptional::getAsByte); }
    public Optional<Character> getAsCharacter() { return getAs().flatMap(JsonElementOptional::getAsCharacter); }
    public Optional<Short> getAsShort() { return getAs().flatMap(JsonElementOptional::getAsShort); }
    public Optional<Boolean> getAsBoolean() { return getAs().flatMap(JsonElementOptional::getAsBoolean); }

    @Override public Object[] createObject() {
        return this.stream().map(v -> v.createObject()).toArray();
    }

    public boolean equals(Object o) { return o == this || o instanceof JsonArrayOptional json && json.elements.equals(this.elements); }
    public int hashCode() { return this.elements.hashCode(); }

    public static JsonArrayOptional of(JsonArray json) {
        JsonArrayOptional result = new JsonArrayOptional();
        json.forEach(kv -> result.add(JsonElementOptional.of(kv)));
        return result;
    }
}
