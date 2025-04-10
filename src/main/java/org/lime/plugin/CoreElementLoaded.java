package org.lime.plugin;

import java.util.Optional;

public interface CoreElementLoaded<T> {
    void cancel();
    Optional<CoreElement<T>> element();
    String name();
    Class<T> type();

    default Optional<T> instance() {
        return element().map(v -> v.instance);
    }

    static <T> CoreElementLoaded<T> disabled(CoreElement<T> element) {
        return new CoreElementLoaded<>() {
            @Override public void cancel() { }
            @Override public Optional<CoreElement<T>> element() { return Optional.empty(); }
            @Override public String name() { return element.name; }
            @Override public Class<T> type() { return element.tClass; }
        };
    }
}
