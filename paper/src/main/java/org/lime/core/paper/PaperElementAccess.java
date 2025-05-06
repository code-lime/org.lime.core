package org.lime.core.paper;

import org.lime.core.common.BaseCoreElementAccess;
import org.lime.core.common.api.elements.BaseCoreElement;

public interface PaperElementAccess
        extends BaseCoreElementAccess<CoreCommand.Register, CoreInstancePlugin.CoreInstance>, PaperJarAccess {
    @Override
    default Class<? super BaseCoreElement<?, CoreCommand.Register, CoreInstancePlugin.CoreInstance, ?>> elementClass() {
        return (Class) CoreElement.class;
    }
}
