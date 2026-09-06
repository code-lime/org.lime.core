package org.lime.core.common.utils.adapters;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Provider;
import org.lime.core.common.api.commands.brigadier.arguments.JsonInput;
import org.lime.core.common.utils.Lazy;

import java.io.IOException;
import java.util.*;

public abstract class JsonTypeAdapter<T, E extends JsonElement>
        extends TypeAdapter<T>
        implements JsonInput.Provider {
    public final Lazy<TypeAdapter<E>> elementTypeAdapter;
    private final Class<E> jsonElementClass;

    public JsonTypeAdapter(Gson gson, Class<E> jsonElementClass) {
        this.jsonElementClass = jsonElementClass;
        this.elementTypeAdapter = Lazy.of(gson.getAdapter(jsonElementClass));
    }
    public JsonTypeAdapter(Provider<Gson> gson, Class<E> jsonElementClass) {
        this.jsonElementClass = jsonElementClass;
        this.elementTypeAdapter = Lazy.of(() -> gson.get().getAdapter(jsonElementClass));
    }

    public abstract E write(T value) throws IOException;
    public abstract T read(E value) throws IOException;

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        elementTypeAdapter.value().write(out, write(value));
    }
    @Override
    public T read(JsonReader in) throws IOException {
        return read(elementTypeAdapter.value().read(in));
    }

    @Override
    public JsonInput.Node input(JsonInput.Context context) {
        if (JsonObject.class.isAssignableFrom(jsonElementClass))
            return JsonInput.object(Map.of(), JsonInput.any(), Set.of());
        if (JsonArray.class.isAssignableFrom(jsonElementClass))
            return JsonInput.array(JsonInput.any());
        if (JsonPrimitive.class.isAssignableFrom(jsonElementClass))
            return JsonInput.choice(List.of(
                    JsonInput.scalar(JsonInput.Type.STRING),
                    JsonInput.scalar(JsonInput.Type.NUMBER),
                    JsonInput.scalar(JsonInput.Type.BOOLEAN)));
        return JsonInput.any();
    }
}
