package org.lime.core.fabric;

import org.lime.core.common.api.scope.BaseKeyedScope;

public abstract class BaseFabricKeyedScope<TKey>
        extends BaseKeyedScope<BaseFabricMod, TKey> {
    protected BaseFabricKeyedScope(BaseFabricMod instance) {
        super(instance);
    }
}
