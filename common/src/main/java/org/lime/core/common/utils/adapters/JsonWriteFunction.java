package org.lime.core.common.utils.adapters;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;

interface JsonWriteFunction<T> {
    void write(JsonWriter writer, T element) throws IOException;
}
