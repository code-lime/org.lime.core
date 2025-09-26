package org.lime.core.common.utils.tuple;

import org.lime.core.common.utils.Lock;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Func1;

import java.util.Arrays;
import java.util.Objects;

public abstract class BaseLockTuple<T extends BaseTuple> {
    private final T Tuple;
    private final Lock lock = Lock.create();

    protected BaseLockTuple(T Tuple) {
        this.Tuple = Tuple;
    }

    public int size() {
        return lock.invoke(Tuple::size);
    }

    protected Object[] getValues() {
        return lock.invoke(Tuple::getValues);
    }

    protected Object get(int index) {
        return lock.invoke(() -> Tuple.get(index));
    }

    protected void set(int index, Object value) {
        lock.invoke(() -> Tuple.set(index, value));
    }

    protected Object edit(int index, Func1<Object, Object> func) {
        return lock.invoke(() -> Tuple.edit(index, func));
    }

    public void invoke(Action1<T> action) {
        lock.invoke(() -> action.invoke(Tuple));
    }

    public <I> I call(Func1<T, I> func) {
        return lock.invoke(() -> func.invoke(Tuple));
    }

    @Override
    public int hashCode() {
        return Objects.hash(11, Objects.hash(getValues()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BaseLockTuple<?> _obj)) return false;
        return _obj.size() == this.size() && Arrays.equals(_obj.getValues(), this.getValues());
    }

    public static <T extends BaseLockTuple<?>> boolean equals(T obj1, T obj2) {
        return Objects.equals(obj1, obj2);
    }
}
