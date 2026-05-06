package org.lime.core.common.utils.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.TypeLiteral;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.execute.Func1;

import java.io.IOException;
import java.lang.reflect.RecordComponent;

public class JsonRecordSingleTypeAdapterFactory
        implements TypeAdapterFactory {
    public static final JsonRecordSingleTypeAdapterFactory INSTANCE = new JsonRecordSingleTypeAdapterFactory();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type == null)
            return null;
        Class<? super T> rawType = type.getRawType();
        if (!rawType.isRecord())
            return null;
        var annotation = rawType.getAnnotation(JsonRecordSingle.class);
        if (annotation == null)
            return null;
        var components = rawType.getRecordComponents();
        if (components.length != 1)
            throw new JsonParseException("Record " + rawType + " marked by " + JsonRecordSingle.class.getSimpleName() + " but contains more than one component: " + components.length);
        var component = components[0];
        return createAdapter(gson, type, TypeToken.get(Reflection.componentType(TypeLiteral.get(type.getType()), component).getType()), component);
    }

    @SuppressWarnings("unchecked")
    private <T, V> TypeAdapter<T> createAdapter(
            Gson gson,
            TypeToken<T> recordType,
            TypeToken<V> parameterType,
            RecordComponent component) {
        Func1<V, T> constructor = ReflectionConstructor.of(recordType.getRawType(), parameterType.getRawType()).lambda(Func1.class);
        Func1<T, V> accessor = ReflectionMethod.of(component.getAccessor()).lambda(Func1.class);

        TypeAdapter<V> parameterAdapter = gson.getAdapter(parameterType);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                parameterAdapter.write(out, accessor.invoke(value));
            }
            @Override
            public T read(JsonReader in) throws IOException {
                return constructor.invoke(parameterAdapter.read(in));
            }
        };
    }
}
