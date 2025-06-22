package org.lime.core.common;

import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.system.execute.Action1;

import java.util.Map;

public interface BaseCoreCommandAccess<Command extends BaseCoreCommandRegister<Self>, Self extends BaseCoreCommandAccess<Command, Self>> {
    void addCommand(Command command);
    default void addCommand(String cmd, Action1<Command> builder) {
        Command command = createCommand(cmd);
        builder.invoke(command);
        addCommand(command);
    }

    Command createCommand(String cmd);
    Map<String, Command> commands();
    void flushCommands();
}
