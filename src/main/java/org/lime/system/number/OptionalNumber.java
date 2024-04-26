package org.lime.system.number;

/*
import com.google.gson.internal.LazilyParsedNumber;
import org.lime.system.execute.Func1;

import java.util.Optional;
public interface OptionalNumber {
    Optional<Integer> intValue();
    Optional<Long> longValue();
    Optional<Float> floatValue();
    Optional<Double> doubleValue();
    Optional<Byte> byteValue();
    Optional<Short> shortValue();

    String toString();

    OptionalNumber EMPTY = new OptionalNumber() {
        @Override public Optional<Integer> intValue() { return Optional.empty(); }
        @Override public Optional<Long> longValue() { return Optional.empty(); }
        @Override public Optional<Float> floatValue() { return Optional.empty(); }
        @Override public Optional<Double> doubleValue() { return Optional.empty(); }
        @Override public Optional<Byte> byteValue() { return Optional.empty(); }
        @Override public Optional<Short> shortValue() { return Optional.empty(); }

        @Override public String toString() { return "0"; }
    };
    static OptionalNumber empty() {
        return EMPTY;
    }

    static OptionalNumber of(Number number) {
        return new OptionalNumber() {
            private <T>Optional<T> tryGetValue(Func1<Number, T> getter) {
                try {
                    return Optional.of(getter.invoke(number));
                } catch (Exception e) {
                    return Optional.empty();
                }
            }

            @Override public Optional<Integer> intValue() { return tryGetValue(Number::intValue); }
            @Override public Optional<Long> longValue() { return tryGetValue(Number::longValue); }
            @Override public Optional<Float> floatValue() { return tryGetValue(Number::floatValue); }
            @Override public Optional<Double> doubleValue() { return tryGetValue(Number::doubleValue); }
            @Override public Optional<Byte> byteValue() { return tryGetValue(Number::byteValue); }
            @Override public Optional<Short> shortValue() { return tryGetValue(Number::shortValue); }

            @Override public String toString() { return number.toString(); }
        };
    }
    static OptionalNumber of(String number) {
        return of(new LazilyParsedNumber(number));
    }
}
*/