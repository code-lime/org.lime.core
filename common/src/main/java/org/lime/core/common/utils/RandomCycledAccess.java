package org.lime.core.common.utils;

import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.tuple.LockTuple1;
import org.lime.core.common.utils.tuple.Tuple;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomCycledAccess<T>
        implements RandomAccess<T> {
    private final Func0<Collection<? extends T>> original;
    private final LockTuple1<LinkedList<T>> cycled;
    private final AtomicInteger cycleIterator = new AtomicInteger(0);

    private RandomCycledAccess(Func0<Collection<? extends T>> original) {
        this.original = original;
        this.cycled = Tuple.lock(createRandomQueue());
    }
    private LinkedList<T> createRandomQueue() {
        var original = this.original.invoke();
        if (original.isEmpty())
            throw new IllegalArgumentException("original of cycled collection is empty");

        LinkedList<T> queue = new LinkedList<>(original);
        Collections.shuffle(queue);
        return queue;
    }

    @Override
    public T next() {
        return cycled.call(v -> {
            var queue = v.val0;
            var item = queue.poll();
            if (item == null) {
                v.val0 = queue = createRandomQueue();
                cycleIterator.incrementAndGet();
                item = queue.remove();
            }
            return item;
        });
    }

    public int cycleIndex() {
        return cycleIterator.get();
    }
    public void skipCycle() {
        cycled.invoke(v -> v.val0.clear());
    }
    public void skipInCycle(Set<T> skips) {
        cycled.invoke(v -> v.val0.removeIf(skips::contains));
    }

    @SafeVarargs
    public final void appendInCycle(T... elements) {
        appendInCycle(Arrays.asList(elements));
    }
    public void appendInCycle(Iterable<T> elements) {
        cycled.invoke(v -> {
            elements.forEach(v.val0::add);
            Collections.shuffle(v.val0);
        });
    }

    public static <T>RandomCycledAccess<T> of(Collection<? extends T> original) {
        return new RandomCycledAccess<>(() -> original);
    }
    public static <T>RandomCycledAccess<T> of(Func0<Collection<? extends T>> original) {
        return new RandomCycledAccess<>(original);
    }
}
