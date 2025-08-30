package org.lime.core.common.api.commands;

import java.util.function.Consumer;

public interface CommandConsumer<Builder> {
    void apply(Builder builder);

    default CommandConsumer<Builder> with(Consumer<Builder> consumer) {
        return new CommandConsumer<>() {
            @Override
            public void apply(Builder builder) {
                CommandConsumer.this.apply(builder);
                consumer.accept(builder);
            }
            @Override
            public Class<Builder> builderClass() {
                return CommandConsumer.this.builderClass();
            }
        };
    }

    Class<Builder> builderClass();
    default void applyCast(Object factory) {
        apply(builderClass().cast(factory));
    }
}
