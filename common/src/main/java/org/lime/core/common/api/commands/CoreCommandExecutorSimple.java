package org.lime.core.common.api.commands;

import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;

public interface CoreCommandExecutorSimple<Self extends CoreCommandExecutorSimple<Self>> {
    Self withExecutorSimple(Func1<String[], Boolean> tab);
    Self withExecutor(Func0<Boolean> executor);
}
