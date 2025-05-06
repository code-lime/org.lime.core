package org.lime.core.common.json;

import com.google.gson.JsonPrimitive;
import org.lime.core.common.system.utils.EnumUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public class JsonPrimitiveOptional extends JsonElementOptional {
    private final JsonPrimitive base;

    public JsonPrimitiveOptional(Boolean value) { this(new JsonPrimitive(value)); }
    public JsonPrimitiveOptional(Number value) { this(new JsonPrimitive(value)); }
    public JsonPrimitiveOptional(String value) { this(new JsonPrimitive(value)); }
    public JsonPrimitiveOptional(Character value) { this(new JsonPrimitive(value)); }

    JsonPrimitiveOptional(JsonPrimitive base) { this.base = base; }

    @Override public JsonPrimitiveOptional deepCopy() { return this; }
    @Override public JsonPrimitive base() { return base; }

    public boolean isBoolean() { return base.isBoolean(); }
    public Optional<Boolean> getAsBoolean() { return base.isBoolean() ? Optional.of(base.getAsBoolean()) : Optional.empty(); }
    public boolean isNumber() { return base.isNumber(); }
    public Optional<Number> getAsNumber() { return base.isNumber() ? Optional.of(base.getAsNumber()) : Optional.empty(); }
    public boolean isString() { return base.isString(); }
    public Optional<String> getAsString() { return Optional.of(base.getAsString()); }
    public <T extends Enum<T>>Optional<T> getAsEnum(Class<T> tClass) { return getAsString().flatMap(v -> EnumUtils.tryParse(tClass, v)); }

    public Optional<Object> getAsObject() { return Optional.empty().or(this::getAsBoolean).or(this::getAsNumber).or(this::getAsString); }
    public Optional<Double> getAsDouble() { return getAsNumber().map(Number::doubleValue); }
    public Optional<Float> getAsFloat() { return getAsNumber().map(Number::floatValue); }
    public Optional<BigDecimal> getAsBigDecimal() {
        try {
            return base.isNumber() ? Optional.of(base.getAsBigDecimal()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    public Optional<BigInteger> getAsBigInteger() {
        try {
            return base.isNumber() ? Optional.of(base.getAsBigInteger()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    public Optional<Long> getAsLong() { return getAsNumber().map(Number::longValue); }
    public Optional<Short> getAsShort() { return getAsNumber().map(Number::shortValue); }
    public Optional<Integer> getAsInt() { return getAsNumber().map(Number::intValue); }
    public Optional<Byte> getAsByte() { return getAsNumber().map(Number::byteValue); }
    public Optional<Character> getAsCharacter() { return getAsString().filter(v -> !v.isEmpty()).map(v -> v.charAt(0)); }

    @Override public Object createObject() { return getAsObject().orElse(null); }

    public int hashCode() { return base.hashCode(); }
    public boolean equals(Object obj) { return obj instanceof JsonPrimitiveOptional json && json.base.equals(base); }

    public static JsonPrimitiveOptional of(JsonPrimitive json) {
        return new JsonPrimitiveOptional(json);
    }
}
