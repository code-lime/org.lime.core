package org.lime.core.common.utils.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public abstract class StringTypeAdapter<T>
        extends TypeAdapter<T> {
    public abstract String write(T value) throws IOException;
    public abstract T read(String value) throws IOException;

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(write(value));
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return read(in.nextString());
    }
}
