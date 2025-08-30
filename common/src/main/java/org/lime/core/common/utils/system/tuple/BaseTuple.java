package org.lime.core.common.utils.system.tuple;

import org.lime.core.common.utils.system.execute.Func1;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class BaseTuple {
    public abstract int size();

    public abstract Object[] getValues();

    public abstract Object get(int index);

    public abstract void set(int index, Object value);

    public abstract Object edit(int index, Func1<Object, Object> func);

    @Override
    public int hashCode() {
        return Objects.hash(size(), 10, Objects.hash(getValues()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BaseTuple _obj)) return false;
        return _obj.size() == this.size() && Arrays.equals(_obj.getValues(), this.getValues());
    }

    public static <T extends BaseTuple> boolean equals(T obj1, T obj2) {
        return Objects.equals(obj1, obj2);
    }

    @Override
    public String toString() {
        return "(" + Arrays.stream(getValues()).map(v -> v + "").collect(Collectors.joining(", ")) + ")";
    }
}
