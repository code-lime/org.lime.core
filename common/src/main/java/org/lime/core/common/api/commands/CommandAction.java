package org.lime.core.common.api.commands;

public interface CommandAction<Sender, Data, Result> {
    Result action(Sender sender, Data data, String[] args);

    default <Other extends Sender>CommandAction<Sender, Data, Boolean> cast(CommandAction<Other, Data, Boolean> executor, Class<Other> sender) {
        return (a,b,c) -> sender.isInstance(a) && executor.action(sender.cast(a),b,c);
    }
}
