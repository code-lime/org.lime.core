package org.lime.core.velocity;

import com.google.inject.Key;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.scope.BaseKeyedScope;

public abstract class BaseVelocityKeyedScope<TKey>
        extends BaseKeyedScope<BaseVelocityPlugin, TKey> {
    protected BaseVelocityKeyedScope(BaseVelocityPlugin instance) {
        super(instance);
    }

    @Override
    protected <T> Composite register(Key<T> elementKey, T element) {
        Composite composite = super.register(elementKey, element);
        if (element instanceof Service service) {
            instance.server.getEventManager().register(this, service);
            composite.add(() -> instance.server.getEventManager().unregisterListener(this, service));
        }
        return composite;
    }
}
