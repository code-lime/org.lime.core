package org.lime.core.common.utils.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public abstract class JsonTypeAdapter<T, E extends JsonElement>
        extends TypeAdapter<T> {
    public final Gson gson;
    public final TypeAdapter<E> elementTypeAdapter;

    public JsonTypeAdapter(Gson gson, Class<E> jsonElementClass) {
        this.gson = gson;
        this.elementTypeAdapter = gson.getAdapter(jsonElementClass);
    }

    public abstract E write(T value) throws IOException;
    public abstract T read(E value) throws IOException;

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        elementTypeAdapter.write(out, write(value));
    }
    @Override
    public T read(JsonReader in) throws IOException {
        return read(elementTypeAdapter.read(in));
    }
}
