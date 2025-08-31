package org.lime.core.common.api.commands;

import java.util.function.Consumer;

public interface CommandConsumer<Register extends CommandConsumer.BaseRegister> {
    void apply(Register register);

    default CommandConsumer<Register> with(Consumer<Register> consumer) {
        return new CommandConsumer<>() {
            @Override
            public void apply(Register register) {
                CommandConsumer.this.apply(register);
                consumer.accept(register);
            }
            @Override
            public Class<Register> registerClass() {
                return CommandConsumer.this.registerClass();
            }
        };
    }

    Class<Register> registerClass();
    default boolean isCast(BaseRegister register) {
        return registerClass().isInstance(register);
    }
    default void applyCast(BaseRegister register) {
        apply(registerClass().cast(register));
    }

    interface BaseRegister {}
}
