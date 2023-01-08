package org.lime.json;

import com.google.gson.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public abstract class JsonElementOptional {
    public abstract JsonElementOptional deepCopy();
    public abstract JsonElement base();

    public boolean isJsonArray() { return this instanceof JsonArrayOptional; }
    public boolean isJsonObject() { return this instanceof JsonObjectOptional; }
    public boolean isJsonPrimitive() { return this instanceof JsonPrimitiveOptional; }
    public boolean isJsonNull() { return this instanceof JsonNullOptional; }

    public Optional<JsonObjectOptional> getAsJsonObject() { return this.isJsonObject() ? Optional.of((JsonObjectOptional)this) : Optional.empty(); }
    public Optional<JsonArrayOptional> getAsJsonArray() { return this.isJsonArray() ? Optional.of((JsonArrayOptional)this) : Optional.empty(); }
    public Optional<JsonPrimitiveOptional> getAsJsonPrimitive() { return this.isJsonPrimitive() ? Optional.of((JsonPrimitiveOptional)this) : Optional.empty(); }
    public Optional<JsonNullOptional> getAsJsonNull() { return this.isJsonNull() ? Optional.of((JsonNullOptional)this) : Optional.empty(); }

    public Optional<Object> getAsObject() { return Optional.empty(); }
    public Optional<Boolean> getAsBoolean() { return Optional.empty(); }
    public Optional<Number> getAsNumber() { return Optional.empty(); }
    public Optional<String> getAsString() { return Optional.empty(); }
    public <T extends Enum<T>>Optional<T> getAsEnum(Class<T> tClass) { return Optional.empty(); }
    public Optional<Double> getAsDouble() { return Optional.empty(); }
    public Optional<Float> getAsFloat() { return Optional.empty(); }
    public Optional<Long> getAsLong() { return Optional.empty(); }
    public Optional<Byte> getAsByte() { return Optional.empty(); }
    public Optional<Integer> getAsInt() { return Optional.empty(); }
    public Optional<Character> getAsCharacter() { return Optional.empty(); }
    public Optional<BigDecimal> getAsBigDecimal() { return Optional.empty(); }
    public Optional<BigInteger> getAsBigInteger() { return Optional.empty(); }
    public Optional<Short> getAsShort() { return Optional.empty(); }

    public String toString() { return base().toString(); }

    public static JsonElementOptional of(JsonElement base) {
        if (base instanceof JsonObject json) return JsonObjectOptional.of(json);
        else if (base instanceof JsonArray json) return JsonArrayOptional.of(json);
        else if (base instanceof JsonNull json || base == null) return JsonNullOptional.of();
        else if (base instanceof JsonPrimitive json) return JsonPrimitiveOptional.of(json);
        throw new IllegalArgumentException("Class '" + base.getClass() + "' not supported!");
    }
}
