package org.lime.system;

import org.lime.system.execute.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class list {
    public static class builder<T> {
        private final List<T> list = new ArrayList<>();

        @SuppressWarnings("all")
        public builder<T> add(T... items) {
            Collections.addAll(list, items);
            return this;
        }

        public builder<T> add(Iterable<T> items) {
            items.forEach(list::add);
            return this;
        }

        public <In> builder<T> add(Iterable<In> items, Func1<In, T> func) {
            items.forEach(v -> list.add(func.invoke(v)));
            return this;
        }

        public List<T> build() {
            return list;
        }

        public builder<T> copy() {
            return new builder<T>().add(list);
        }
    }

    public static <T> builder<T> of(Class<T> tClass) {
        return new builder<>();
    }

    public static <T> builder<T> of() {
        return new builder<>();
    }
}
