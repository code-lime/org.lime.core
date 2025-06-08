package org.lime.core.velocity;

import org.lime.core.common.api.elements.BaseCoreElement;

public final class CoreElement<T>
        extends BaseCoreElement<T, CoreCommand.Register, CoreInstance, CoreElement<T>> {
    @Override
    protected CoreElement<T> self() {
        return this;
    }

    CoreElement(Class<T> tClass) {
        super(tClass);
    }

    @Override
    protected void $register(CoreInstance instance) {

    }

    @Override
    public CoreCommand.Register command(String cmd) {
        return CoreCommand.Register.create(cmd);
    }

    public static <T>CoreElement<T> create(Class<T> tClass) {
        return new CoreElement<>(tClass);
    }
}
