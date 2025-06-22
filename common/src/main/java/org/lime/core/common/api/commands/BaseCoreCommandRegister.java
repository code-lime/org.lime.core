package org.lime.core.common.api.commands;

import org.lime.core.common.api.BaseLogger;

public interface BaseCoreCommandRegister<Owner, Self extends BaseCoreCommandRegister<Owner, Self>> {
    String cmd();
    Self join(Self command, BaseLogger logger);
    void register(Owner owner);
}
