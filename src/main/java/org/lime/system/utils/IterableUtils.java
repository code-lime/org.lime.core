package org.lime.system.utils;

import org.lime.system.execute.Action1;
import org.lime.system.execute.Action2;
import org.lime.system.execute.Func0;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.system.toast.Toast3;

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
    public static <T, TAny, TRet>void waitAllAnyAsyns(Collection<Toast2<T, TAny>> list, Action2<T, Action1<TRet>> func, Action1<List<Toast3<T, TAny, TRet>>> callback) {
        var locked = Toast.lock(0);
        List<Toast3<T, TAny, TRet>> ret = new ArrayList<>();
        for (Toast2<T, TAny> item : list) {
            locked.edit0(v -> v + 1);
            int index = ret.size();
            ret.add(Toast.of(item.val0, item.val1, null));
            func.invoke(item.val0, v -> {
                ret.get(index).val2 = v;
                if (locked.edit0(j -> j - 1) > 0) return;
                callback.invoke(ret);
            });
        }
        if (ret.size() == 0) callback.invoke(ret);
    }

    public static <T>Iterable<T> iterable(Stream<T> stream) {
        return stream::iterator;
    }
    public static <T>Iterable<T> iterable(Func0<Stream<T>> streamGetter) {
        return () -> streamGetter.invoke().iterator();
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

    public static <T>Stream<Toast2<Integer, T>> streamIndexed(T[] array) {
        return IntStream.range(0, array.length).mapToObj(i -> Toast.of(i, array[i]));
    }
    public static <T>Stream<Toast2<Integer, T>> streamIndexed(List<T> list) {
        return IntStream.range(0, list.size()).mapToObj(i -> Toast.of(i, list.get(i)));
    }
}
