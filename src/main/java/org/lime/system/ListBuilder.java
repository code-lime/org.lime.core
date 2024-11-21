package org.lime.system;

import org.lime.system.execute.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListBuilder<T> {
    private final List<T> list = new ArrayList<>();

    @SuppressWarnings("all")
    public ListBuilder<T> add(T... items) {
        Collections.addAll(list, items);
        return this;
    }

    public ListBuilder<T> add(Iterable<T> items) {
        items.forEach(list::add);
        return this;
    }

    public <In> ListBuilder<T> add(Iterable<In> items, Func1<In, T> func) {
        items.forEach(v -> list.add(func.invoke(v)));
        return this;
    }

    public List<T> build() {
        return list;
    }

    public ListBuilder<T> copy() {
        return new ListBuilder<T>().add(list);
    }

    public static <T> ListBuilder<T> of(Class<T> ignored) {
        return new ListBuilder<>();
    }

    public static <T> ListBuilder<T> of() {
        return new ListBuilder<>();
    }
}
