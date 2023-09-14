package org.lime.plugin;

import java.util.Optional;

public interface CoreElementLoaded {
    void cancel();
    Optional<CoreElement> element();
    String name();
    Class<?> type();

    static CoreElementLoaded disabled(CoreElement element) {
        return new CoreElementLoaded() {
            @Override public void cancel() { }
            @Override public Optional<CoreElement> element() { return Optional.empty(); }
            @Override public String name() { return element.name; }
            @Override public Class<?> type() { return element.tClass; }
        };
    }
}
