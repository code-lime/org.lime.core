package net.minecraft.paper.java.view;

import java.util.Iterator;
import java.util.function.Function;

public class OtherViewIterator<I extends V,V> implements Iterator<V> {
    protected final Iterator<I> other;
    protected final Function<V,V> modify;

    public OtherViewIterator(Iterator<I> other, Function<V,V> modify) {
        this.other = other;
        this.modify = modify;
    }

    @Override
    public boolean hasNext() {
        return other.hasNext();
    }
    @Override
    public V next() {
        return modify.apply(other.next());
    }
}
