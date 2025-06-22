package org.lime.core.common.api.commands;

public interface BaseCoreCommandRegister<Owner> {
    String cmd();
    void register(Owner owner);
}
