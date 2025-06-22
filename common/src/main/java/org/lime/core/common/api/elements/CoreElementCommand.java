package org.lime.core.common.api.elements;

import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.system.execute.Action1;

public interface CoreElementCommand<Command extends BaseCoreCommandRegister<Owner>, Owner, Self extends CoreElementCommand<Command, Owner, Self>> {
    Command command(String cmd);

    Self addCommands(Command... commands);

    default Self addCommand(Command command) {
        return addCommands(command);
    }
    default Self addCommand(String cmd, Action1<Command> command) {
        Command value = command(cmd);
        command.invoke(value);
        return addCommands(value);
    }
}
