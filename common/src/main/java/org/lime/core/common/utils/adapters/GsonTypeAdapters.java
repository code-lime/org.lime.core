package org.lime.core.common.utils.adapters;

import com.google.gson.TypeAdapterFactory;

import java.util.stream.Stream;

public interface GsonTypeAdapters {
    Stream<TypeAdapterFactory> factories();
}
