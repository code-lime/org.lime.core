package org.lime.core.common.utils.json.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.lime.core.common.utils.execute.Func1;

import java.util.Iterator;

public class ArrayBuilder extends BaseBuilder<JsonArray> {
    private final JsonArray json;

    ArrayBuilder(JsonArray json) {
        this.json = json;
    }

    ArrayBuilder() {
        this(new JsonArray());
    }

    public <T> ArrayBuilder add(Iterable<T> items) {
        items.forEach(this::add);
        return this;
    }

    public <In, T> ArrayBuilder add(Iterable<In> items, Func1<In, T> format) {
        items.forEach(item -> add(format.invoke(item)));
        return this;
    }

    public <T> ArrayBuilder add(Iterator<T> items) {
        items.forEachRemaining(this::add);
        return this;
    }

    public <In, T> ArrayBuilder add(Iterator<In> items, Func1<In, T> format) {
        items.forEachRemaining(item -> add(format.invoke(item)));
        return this;
    }

    public <In, T> ArrayBuilder add(In[] items, Func1<In, T> format) {
        for (In item : items) add(format.invoke(item));
        return this;
    }

    public ArrayBuilder add(JsonElement value) {
        json.add(value);
        return this;
    }

    public ArrayBuilder addObject(Func1<ObjectBuilder, ObjectBuilder> value) {
        return add(value.invoke(new ObjectBuilder()));
    }

    public ArrayBuilder addArray(Func1<ArrayBuilder, ArrayBuilder> value) {
        return add(value.invoke(new ArrayBuilder()));
    }

    public ArrayBuilder addNull() {
        return add(byObject(null));
    }

    public ArrayBuilder add(BaseBuilder<?> value) {
        return add(value.build());
    }

    public ArrayBuilder add(Object value) {
        return add(byObject(value));
    }

    @Override
    public JsonArray build() {
        return json;
    }
}
