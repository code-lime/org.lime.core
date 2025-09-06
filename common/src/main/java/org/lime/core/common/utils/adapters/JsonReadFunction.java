package org.lime.core.common.utils.adapters;

import com.google.gson.stream.JsonReader;

import java.io.IOException;

interface JsonReadFunction<T> {
    T read(JsonReader reader) throws IOException;
}
