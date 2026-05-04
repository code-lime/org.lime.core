package org.lime.core.common.utils;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collector;

public interface Disposable
        extends Closeable {
    Disposable EMPTY = () -> {};
    Collector<Disposable, Composite, Composite> COLLECTOR = Collector.of(Disposable::composite, Composite::add, Composite::add);

    @Override
    void close();

    static Disposable of(AutoCloseable closeable) {
        return () -> {
            try {
                closeable.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
    static Disposable.Composite combine(Collection<? extends Disposable> disposables) {
        return new Composite() {
            final ConcurrentLinkedDeque<Disposable> deque = new ConcurrentLinkedDeque<>(disposables);

            @Override
            public Composite add(Disposable disposable) {
                deque.add(disposable);
                return this;
            }
            @Override
            public Composite addAll(Collection<? extends Disposable> disposables) {
                deque.addAll(disposables);
                return this;
            }

            @Override
            public void close() {
                Disposable disposable;
                while ((disposable = deque.pollLast()) != null) {
                    disposable.close();
                }
            }
        };
    }
    static Disposable.Composite combine(Disposable... disposables) {
        return combine(List.of(disposables));
    }
    static Disposable.Composite composite() {
        return combine(Collections.emptyList());
    }
    static Disposable empty() {
        return EMPTY;
    }

    static Collector<Disposable, Composite, Composite> toDisposable() {
        return COLLECTOR;
    }

    interface Composite
            extends Disposable {
        Composite add(Disposable disposable);
        Composite addAll(Collection<? extends Disposable> disposables);
    }
}
