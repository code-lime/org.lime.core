package net.minecraft.paper.java.view;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public class OtherViewCollection<V> implements Collection<V> {
    protected final Collection<? extends V> other;
    protected final Function<V,V> modify;

    public OtherViewCollection(Collection<? extends V> other, Function<V,V> modify) {
        this.other = other;
        this.modify = modify;
    }

    @Override
    public int size() {
        return other.size();
    }
    @Override
    public boolean isEmpty() {
        return other.isEmpty();
    }
    @Override
    public boolean contains(Object o) {
        return other.contains(o);
    }
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object e : c)
            if (!contains(e))
                return false;
        return true;
    }

    @Override
    public @NotNull Iterator<V> iterator() {
        return new OtherViewIterator<>(other.iterator(), modify);
    }
    @Override public Object[] toArray() {
        return stream().toArray();
    }
    @SuppressWarnings("all")
    @Override
    public <T> T[] toArray(T[] a) {
        Class<T> type = (Class<T>)a.getClass().getComponentType();
        return stream().toArray(length -> (T[]) Array.newInstance(type, length));
    }

    @Override
    public boolean add(V v) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean addAll(@NotNull Collection<? extends V> c) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
