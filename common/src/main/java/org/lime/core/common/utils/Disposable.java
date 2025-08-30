package org.lime.core.common.utils;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public interface Disposable
        extends Closeable {
    Disposable EMPTY = () -> {};

    @Override
    void close();

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

    interface Composite
            extends Disposable {
        Composite add(Disposable disposable);
        Composite addAll(Collection<? extends Disposable> disposables);
    }
}
