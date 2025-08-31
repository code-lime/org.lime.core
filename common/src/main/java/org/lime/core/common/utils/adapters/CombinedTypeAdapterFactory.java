package org.lime.core.common.utils.adapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public record CombinedTypeAdapterFactory(
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
