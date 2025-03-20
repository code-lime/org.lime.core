package net.minecraft.paper.java.view;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class OtherViewIteratorEntry<K,I extends V,V> implements Iterator<Map.Entry<K,V>> {
    protected final Iterator<Map.Entry<K,I>> other;
    protected final Function<V,V> modify;

    public OtherViewIteratorEntry(Iterator<Map.Entry<K,I>> other, Function<V,V> modify) {
        this.other = other;
        this.modify = modify;
    }

    @Override
    public boolean hasNext() {
        return other.hasNext();
    }
    @Override
    public Map.Entry<K,V> next() {
        return new OtherViewEntry<>(other.next(), modify);
    }
}
