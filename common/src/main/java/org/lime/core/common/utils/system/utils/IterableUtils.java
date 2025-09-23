package org.lime.core.common.utils.system.utils;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.system.execute.Action1;
import org.lime.core.common.utils.system.execute.Action2;
import org.lime.core.common.utils.system.execute.Action3;
import org.lime.core.common.utils.system.execute.Func0;
import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple2;
import org.lime.core.common.utils.system.tuple.Tuple3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IterableUtils {
    public static <T>T getOrDefault(List<T> list, int index, T def) {
        return index < list.size() ? list.get(index) : def;
    }
    public static <T, TAny, TRet>void waitAllAnyAsyns(Collection<Tuple2<T, TAny>> list, Action2<T, Action1<TRet>> func, Action1<List<Tuple3<T, TAny, TRet>>> callback) {
        var locked = Tuple.lock(0);
        List<Tuple3<T, TAny, TRet>> ret = new ArrayList<>();
        for (Tuple2<T, TAny> item : list) {
            locked.edit0(v -> v + 1);
            int index = ret.size();
            ret.add(Tuple.of(item.val0, item.val1, null));
            func.invoke(item.val0, v -> {
                ret.get(index).val2 = v;
                if (locked.edit0(j -> j - 1) > 0) return;
                callback.invoke(ret);
            });
        }
        if (ret.isEmpty()) callback.invoke(ret);
    }

    public static <T>Iterable<T> iterable(Stream<T> stream) {
        return stream::iterator;
    }
    public static <T>Iterable<T> iterable(Func0<Stream<T>> streamGetter) {
        return () -> streamGetter.invoke().iterator();
    }
    public static <T>Iterable<T> shadow(Func0<Iterable<T>> iterableGetter) {
        return () -> iterableGetter.invoke().iterator();
    }

    public static <T> Stream<T> skipLast(Stream<T> s, int count) {
        if(count <= 0) {
            if(count == 0) return s;
            throw new IllegalArgumentException(count + " < 0");
        }
        ArrayDeque<T> pending = new ArrayDeque<>(count+1);
        Spliterator<T> src=s.spliterator();
        return StreamSupport.stream(new Spliterator<T>() {
            public boolean tryAdvance(Consumer<? super T> action) {
                while(pending.size() <= count && src.tryAdvance(pending::add));
                if (pending.size() <= count) return false;
                action.accept(pending.remove());
                return true;
            }

            public Spliterator<T> trySplit() { return null; }
            public long estimateSize() { return src.estimateSize()-count; }
            public int characteristics() { return src.characteristics(); }
        }, false);
    }

    public static <T> Stream<T> reverse(Stream<T> stream) {
        LinkedList<T> stack = new LinkedList<>();
        stream.forEach(stack::push);
        return stack.stream();
    }
    public static <T, TL extends Collection<T>>TL distinct(TL list) {
        LinkedHashSet<T> set = new LinkedHashSet<>(list);
        list.clear();
        list.addAll(set);
        return list;
    }
    public static <T> Predicate<T> distinctBy(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static void removeAll(Iterator<?> iterator) {
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public static <T>Stream<Tuple2<Integer, T>> streamIndexed(T[] array) {
        return IntStream.range(0, array.length).mapToObj(i -> Tuple.of(i, array[i]));
    }
    public static <T>Stream<Tuple2<Integer, T>> streamIndexed(List<T> list) {
        return IntStream.range(0, list.size()).mapToObj(i -> Tuple.of(i, list.get(i)));
    }

    public static <T>void progress(Collection<T> list, Action2<T, String> invoke) {
        int size = list.size();
        int i = 0;
        for (T item : list) {
            invoke.invoke(item, "[" + StringUtils.leftPad(String.valueOf(i*100 / size), 3, '*').replace("*", "...") + "%]");
            i++;
        }
    }
    public static <K, V>void progress(Map<K, V> list, Action3<K, V, String> invoke) {
        progress(list.entrySet(), (v, pref) -> invoke.invoke(v.getKey(), v.getValue(), pref));
    }

    @SafeVarargs
    public static <T extends @Nullable Object> Collection<T> concat(Collection<? extends T>... inputs) {
        return new CombineCollection<>(inputs);
    }

    private static class CombineCollection<T> extends AbstractCollection<T> {
        private final Collection<? extends T>[] inputs;
        @SafeVarargs
        public CombineCollection(Collection<? extends T>... inputs) {
            this.inputs = inputs;
        }
        @Override
        public int size() {
            int size = 0;
            for (var input : inputs)
                size += input.size();
            return size;
        }
        @Override
        public boolean isEmpty() {
            for (var input : inputs)
                if (!input.isEmpty())
                    return false;
            return true;
        }
        @Override
        public boolean contains(Object o) {
            for (var input : inputs)
                if (input.contains(o))
                    return true;
            return false;
        }
        @Override
        public @NotNull Iterator<T> iterator() {
            return Iterables.concat(inputs).iterator();
        }
        @Override
        public boolean remove(Object o) {
            for (var input : inputs)
                if (input.remove(o))
                    return true;
            return false;
        }
        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            for (Object e : c)
                if (!contains(e))
                    return false;
            return true;
        }
        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            boolean modified = false;
            for (var input : inputs)
                modified = input.removeAll(c) || modified;
            return modified;
        }
        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            boolean modified = false;
            for (var input : inputs)
                modified = input.retainAll(c) || modified;
            return modified;
        }
        @Override
        public void clear() {
            for (var input : inputs)
                input.clear();
        }
    }
}
