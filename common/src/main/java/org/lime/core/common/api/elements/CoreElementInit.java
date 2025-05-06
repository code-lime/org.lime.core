package org.lime.core.common.api.elements;

import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.system.execute.Action0;
import org.lime.core.common.system.execute.Action1;

public interface CoreElementInit<Command extends BaseCoreCommandRegister<Owner, Command>, Owner, Self extends CoreElementInit<Command, Owner, Self>> {
    Self withInit(Action1<Owner> init);

    default Self withInit(Action0 init) {
        return withInit(_ -> init.invoke());
    }
}
