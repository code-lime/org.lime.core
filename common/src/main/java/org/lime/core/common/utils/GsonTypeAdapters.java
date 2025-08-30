package org.lime.core.common.utils;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.DoubleRange;
import org.apache.commons.lang3.IntegerRange;
import org.apache.commons.lang3.LongRange;
import org.apache.commons.lang3.Range;
import org.lime.core.common.utils.typeadapers.StringTypeAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.function.BiFunction;

public class GsonTypeAdapters {
    private record CombinedTypeAdapterFactory(
            TypeAdapterFactory... factories)
            implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            for (TypeAdapterFactory factory : factories) {
                TypeAdapter<T> adapter = factory.create(gson, type);
                if (adapter != null)
                    return adapter;
            }
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <T, J extends T>TypeToken<J> getParameterized(Class<T> rawClass, Type... typeArguments) {
        return (TypeToken<J>)TypeToken.getParameterized(rawClass, typeArguments);
    }
    private static TypeAdapterFactory combine(TypeAdapterFactory... factories) {
        return new CombinedTypeAdapterFactory(factories);
    }

    public static <T>TypeAdapterFactory key() {
        final TypeAdapter<Key> keyTypeAdapter = new StringTypeAdapter<>() {
            @Override
            public String write(Key value) throws IOException {
                return value.asString();
            }
            @Override
            public Key read(String value) throws IOException {
                return Key.key(value);
            }
        };
        return TypeAdapters.newTypeHierarchyFactory(Key.class, keyTypeAdapter);
    }
    public static TypeAdapterFactory miniMessage(
            MiniMessage miniMessage) {
        final TypeAdapter<Component> componentTypeAdapter = new StringTypeAdapter<>() {
            @Override
            public String write(Component value) throws IOException {
                return miniMessage.serialize(value);
            }
            @Override
            public Component read(String value) throws IOException {
                return miniMessage.deserialize(value);
            }
        };
        final TypeAdapter<PlaceholderComponent> placeholderComponentTypeAdapter = new StringTypeAdapter<>() {
            @Override
            public String write(PlaceholderComponent value) throws IOException {
                return value.rawComponent();
            }
            @Override
            public PlaceholderComponent read(String value) throws IOException {
                return new PlaceholderComponent(miniMessage, value);
            }
        };
        return combine(
                TypeAdapters.newFactory(Component.class, componentTypeAdapter),
                TypeAdapters.newFactory(PlaceholderComponent.class, placeholderComponentTypeAdapter));
    }
    public static TypeAdapterFactory duration() {
        return TypeAdapters.newFactory(
                Duration.class,
                new StringTypeAdapter<>() {
                    private static void addPart(StringBuilder builder, long value, String suffix) {
                        if (value <= 0)
                            return;
                        builder.append(value).append(suffix);
                    }
                    @Override
                    public String write(Duration value) {
                        if (value.isNegative())
                            return "0s";

                        StringBuilder builder = new StringBuilder();

                        addPart(builder, value.toSecondsPart(), "s");
                        addPart(builder, value.toMinutesPart(), "m");
                        addPart(builder, value.toHoursPart(), "h");
                        addPart(builder, value.toDays(), "d");

                        if (builder.isEmpty())
                            builder.append("0s");

                        return builder.toString();
                    }
                    @Override
                    public Duration read(String value) {
                        int amount = 0;
                        Duration duration = Duration.ZERO;

                        for (char ch : value.toCharArray()) {
                            if ('0' <= ch && ch <= '9') {
                                amount = amount * 10 + (ch - '0');
                            } else {
                                TemporalUnit unit = switch (ch) {
                                    case 's' -> ChronoUnit.SECONDS;
                                    case 'm' -> ChronoUnit.MINUTES;
                                    case 'h' -> ChronoUnit.HOURS;
                                    case 'd' -> ChronoUnit.DAYS;
                                    default -> throw new IllegalStateException("Unexpected character: " + ch);
                                };
                                duration = duration.plus(amount, unit);
                                amount = 0;
                            }
                        }

                        if (amount > 0)
                            duration = duration.plusSeconds(amount);

                        return duration;
                    }
                });
    }

    private static <T, R extends Range<T>>TypeAdapterFactory range(
            Class<R> rangeClass,
            BiFunction<T, T, R> creator,
            JsonReadFunction<T> readElement,
            JsonWriteFunction<T> writeElement) {
        return TypeAdapters.newFactory(
                rangeClass,
                new TypeAdapter<>() {
                    @Override
                    public void write(JsonWriter out, R value) throws IOException {
                        out.beginArray();
                        writeElement.write(out, value.getMinimum());
                        writeElement.write(out, value.getMaximum());
                        out.endArray();
                    }
                    @Override
                    public R read(JsonReader in) throws IOException {
                        in.beginArray();
                        T a = readElement.read(in);
                        T b = readElement.read(in);
                        in.endArray();
                        return creator.apply(a, b);
                    }
                });
    }
    public static TypeAdapterFactory range() {
        return combine(
                range(IntegerRange.class, IntegerRange::of, JsonReader::nextInt, JsonWriter::value),
                range(DoubleRange.class, DoubleRange::of, JsonReader::nextDouble, JsonWriter::value),
                range(LongRange.class, LongRange::of, JsonReader::nextLong, JsonWriter::value));
    }
}
