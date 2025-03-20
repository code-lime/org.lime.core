package net.minecraft.paper.java.view;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class OtherView {
    public static <V>Map<?,V> ofValues(Map<?,? extends V> other, Function<V,V> modify) {
        return new OtherViewMap<>(other, modify);
    }
    public static <K,V>Map<K,V> of(Map<K,? extends V> other, Function<V,V> modify) {
        return new OtherViewMap<>(other, modify);
    }
    public static <V>Collection<V> of(Collection<? extends V> other, Function<V,V> modify) {
        return new OtherViewCollection<>(other, modify);
    }
    public static <V>Set<V> of(Set<? extends V> other, Function<V,V> modify) {
        return new OtherViewSet<>(other, modify);
    }
    public static <V>Iterator<V> of(Iterator<? extends V> other, Function<V,V> modify) {
        return new OtherViewIterator<>(other, modify);
    }
}
