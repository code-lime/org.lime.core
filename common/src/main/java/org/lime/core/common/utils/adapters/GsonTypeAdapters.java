package org.lime.core.common.utils.adapters;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.lime.core.common.utils.PlaceholderComponent;
import org.lime.core.common.utils.range.*;
import org.lime.core.common.utils.system.execute.ActionEx2;
import org.lime.core.common.utils.system.execute.FuncEx1;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;

public class GsonTypeAdapters {
    @SuppressWarnings({"unchecked", "SameParameterValue"})
    public static <T, J extends T>TypeToken<J> getParameterized(Class<T> rawClass, Type... typeArguments) {
        return (TypeToken<J>)TypeToken.getParameterized(rawClass, typeArguments);
    }
    public static TypeAdapterFactory combine(TypeAdapterFactory... factories) {
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
                    @Override
                    public String write(Duration value) {
                        return DurationUtils.write(value);
                    }
                    @Override
                    public Duration read(String value) {
                        return DurationUtils.read(value);
                    }
                });
    }

    public static <T extends Comparable<T>, R extends Range<T>>TypeAdapterFactory range(
            Range.Factory<R, T> factory,
            FuncEx1<JsonReader, T> readElement,
            ActionEx2<JsonWriter, T> writeElement) {
        return TypeAdapters.newFactory(
                factory.rangeClass(),
                new TypeAdapter<>() {
                    @Override
                    public void write(JsonWriter out, R value) throws IOException {
                        try {
                            out.beginArray();
                            writeElement.invoke(out, value.min());
                            writeElement.invoke(out, value.max());
                            out.endArray();
                        } catch (IOException e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }
                    @Override
                    public R read(JsonReader in) throws IOException {
                        try {
                            in.beginArray();
                            T a = readElement.invoke(in);
                            T b = readElement.invoke(in);
                            in.endArray();
                            return factory.create(a, b);
                        } catch (IOException e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
    public static TypeAdapterFactory range() {
        return combine(
                range(DurationRange.FACTORY, v -> DurationUtils.read(v.nextString()), (w,v) -> w.value(DurationUtils.write(v))),
                range(FloatRange.FACTORY, v -> (float)v.nextDouble(), JsonWriter::value),
                range(IntegerRange.FACTORY, JsonReader::nextInt, JsonWriter::value),
                range(DoubleRange.FACTORY, JsonReader::nextDouble, JsonWriter::value),
                range(LongRange.FACTORY, JsonReader::nextLong, JsonWriter::value));
    }
}
