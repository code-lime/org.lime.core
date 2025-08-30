package org.lime.core.common.utils.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class EnumFlag {
    protected abstract long getBit();

    public static <T extends EnumFlag> boolean has(long bit, T type) {
        return (bit & type.getBit()) == type.getBit();
    }
    public static <T extends EnumFlag> List<T> from(long bit, Collection<T> values) {
        List<T> list = new ArrayList<>();
        for (T type : values) {
            if (has(bit, type))
                list.add(type);
        }
        return list;
    }
    public static <T extends EnumFlag> long from(Collection<T> bits) {
        long b = 0;
        for (EnumFlag type : bits) b |= type.getBit();
        return b;
    }
    public static <T extends EnumFlag> long add(List<T> bits, T bit) {
        return add(from(bits), bit);
    }
    public static <T extends EnumFlag> long del(List<T> bits, T bit) {
        return del(from(bits), bit);
    }
    public static <T extends EnumFlag> long add(long bits, T bit) {
        bits |= bit.getBit();
        return bits;
    }
    public static <T extends EnumFlag> long del(long bits, T bit) {
        bits &= ~bit.getBit();
        return bits;
    }
}
