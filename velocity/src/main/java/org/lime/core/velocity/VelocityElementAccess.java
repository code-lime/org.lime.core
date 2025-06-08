package org.lime.core.velocity;

import org.lime.core.common.BaseCoreElementAccess;
import org.lime.core.common.api.elements.BaseCoreElement;

public interface VelocityElementAccess
        extends BaseCoreElementAccess<CoreCommand.Register, CoreInstance>, VelocityJarAccess {
    @Override
    default Class<? super BaseCoreElement<?, CoreCommand.Register, CoreInstance, ?>> elementClass() {
        return (Class)CoreElement.class;
    }
}
