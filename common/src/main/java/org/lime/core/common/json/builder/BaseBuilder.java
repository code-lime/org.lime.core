package org.lime.core.common.json.builder;

import com.google.gson.JsonElement;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseBuilder<T extends JsonElement> {
    private static List<Object> toList(Object array) {
        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < length; i++) list.add(Array.get(array, i));
        return list;
    }

    public static BaseBuilder<?> byObject(Object value) {
        if (value == null) return ElementBuilder.create();
        else if (value instanceof BaseBuilder<?> dat) return dat;
        else if (value instanceof String dat) return ElementBuilder.create(dat);
        else if (value instanceof Number dat) return ElementBuilder.create(dat);
        else if (value instanceof Boolean dat) return ElementBuilder.create(dat);
        else if (value instanceof Character dat) return ElementBuilder.create(dat);
        else if (value instanceof Enum<?> dat) return ElementBuilder.create(dat.name());
        else if (value instanceof JsonElement dat) return ElementBuilder.create(dat);
        else if (value instanceof Map<?, ?> dat) return Json.object().add(dat, String::valueOf, v -> v);
        else if (value instanceof Iterable<?> dat) return Json.array().add(dat, v -> v);
        else if (value.getClass().isArray()) return byObject(toList(value));
        else return ElementBuilder.create(value.toString());
    }

    public abstract T build();

    @Override
    public String toString() {
        return build().toString();
    }
}
