package org.lime.core.common.docs;

import com.google.common.collect.Streams;

import org.jetbrains.annotations.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public interface IParent {
    @Nullable IParent parent();

    static Stream<IParent> parentTree(IParent parent) {
        return Streams.stream(new Iterator<IParent>() {
            private boolean first = true;
            private IParent element = null;
            @Override public boolean hasNext() {
                if (!first)
                    return element != null;
                first = false;
                element = parent;
                return true;
            }
            @Override public IParent next() {
                if (first || element == null)
                    throw new NoSuchElementException();

                IParent next = element;
                element = next.parent();
                return next;
            }
        });
    }
}
