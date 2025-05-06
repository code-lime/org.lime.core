package org.lime.core.common.api;

import org.lime.core.common.BaseCoreInstance;

public interface ElementInstance {
    void core(BaseCoreInstance<?,?> baseCore);
    BaseCoreInstance<?,?> core();
}
