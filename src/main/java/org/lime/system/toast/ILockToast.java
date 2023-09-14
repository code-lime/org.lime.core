package org.lime.system.toast;

import org.lime.system.Lock;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func1;

import java.util.Arrays;
import java.util.Objects;

public abstract class ILockToast<T extends IToast> {
    private final T toast;
    private final Lock lock = Lock.create();

    protected ILockToast(T toast) {
        this.toast = toast;
    }

    public int size() {
        return lock.invoke(toast::size);
    }

    protected Object[] getValues() {
        return lock.invoke(toast::getValues);
    }

    protected Object get(int index) {
        return lock.invoke(() -> toast.get(index));
    }

    protected void set(int index, Object value) {
        lock.invoke(() -> toast.set(index, value));
    }

    protected Object edit(int index, Func1<Object, Object> func) {
        return lock.invoke(() -> toast.edit(index, func));
    }

    public void invoke(Action1<T> action) {
        lock.invoke(() -> action.invoke(toast));
    }

    public <I> I call(Func1<T, I> func) {
        return lock.invoke(() -> func.invoke(toast));
    }

    @Override
    public int hashCode() {
        return Objects.hash(11, Objects.hash(getValues()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ILockToast<?> _obj)) return false;
        return _obj.size() == this.size() && Arrays.equals(_obj.getValues(), this.getValues());
    }

    public static <T extends ILockToast<?>> boolean equals(T obj1, T obj2) {
        return Objects.equals(obj1, obj2);
    }
}
