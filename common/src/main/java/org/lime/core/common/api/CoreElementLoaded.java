package org.lime.core.common.api;

import org.lime.core.common.api.elements.BaseCoreElement;

import java.util.Optional;

public interface CoreElementLoaded<T, Element extends BaseCoreElement<T, ?, ?, Element>> {
    void cancel();
    Optional<Element> element();
    String name();
    Class<T> type();

    default Optional<T> instance() {
        return element().map(v -> v.instance);
    }

    static <T, Element extends BaseCoreElement<T,?,?,Element>> CoreElementLoaded<T, Element> disabled(Element element) {
        return new CoreElementLoaded<>() {
            @Override public void cancel() { }
            @Override public Optional<Element> element() { return Optional.empty(); }
            @Override public String name() { return element.name; }
            @Override public Class<T> type() { return element.tClass; }
        };
    }
}
