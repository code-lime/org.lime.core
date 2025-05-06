package org.lime.core.fabric;

import org.lime.core.common.BaseCoreElementAccess;
import org.lime.core.common.api.elements.BaseCoreElement;

public interface FabricElementAccess
        extends BaseCoreElementAccess<CoreCommand.Register, CoreInstance>, FabricJarAccess {
    @Override
    default Class<? super BaseCoreElement<?, CoreCommand.Register, CoreInstance, ?>> elementClass() {
        return (Class)CoreElement.class;
    }
}
