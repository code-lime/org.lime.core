package org.lime.core.common.utils;

import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.tuple.LockTuple1;
import org.lime.core.common.utils.tuple.Tuple;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class RandomCycledAccess<T> {
    private final Func0<Collection<? extends T>> original;
    private final LockTuple1<Queue<T>> cycled;

    private RandomCycledAccess(Func0<Collection<? extends T>> original) {
        this.original = original;
        this.cycled = Tuple.lock(createRandomQueue());
    }
    private Queue<T> createRandomQueue() {
        var original = this.original.invoke();
        if (original.isEmpty())
            throw new IllegalArgumentException("original of cycled collection is empty");

        LinkedList<T> queue = new LinkedList<>(original);
        Collections.shuffle(queue);
        return queue;
    }

    public T next() {
        return cycled.call(v -> {
            var queue = v.val0;
            var item = queue.poll();
            if (item == null) {
                v.val0 = queue = createRandomQueue();
                item = queue.remove();
            }
            return item;
        });
    }

    public static <T>RandomCycledAccess<T> of(Collection<? extends T> original) {
        return new RandomCycledAccess<>(() -> original);
    }
    public static <T>RandomCycledAccess<T> of(Func0<Collection<? extends T>> original) {
        return new RandomCycledAccess<>(original);
    }
}
