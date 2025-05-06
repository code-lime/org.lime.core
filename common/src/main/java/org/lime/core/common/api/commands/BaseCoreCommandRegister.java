package org.lime.core.common.api.commands;

public interface BaseCoreCommandRegister<
        Owner,
        Self extends BaseCoreCommandRegister<Owner, Self>> {
    String cmd();
    void register(Owner owner);
}
