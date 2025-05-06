package org.lime.core.common.api.commands;

import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.execute.Func2;

public interface CoreCommandExecutor<Sender, Data, Self extends CoreCommandExecutor<Sender, Data, Self>>
        extends CoreCommandExecutorSimple<Self> {
    Self withExecutor(CommandAction<Sender, Data, Boolean> executor);

    @Override
    default Self withExecutorSimple(Func1<String[], Boolean> executor) {
        return withExecutor((_, args) -> executor.invoke(args));
    }
    default Self withExecutor(Func2<Sender, String[], Boolean> executor) {
        return withExecutor((v0, _, v3) -> executor.invoke((Sender) v0, v3));
    }
    default Self withExecutor(Func1<Sender, Boolean> executor) {
        return withExecutor((v0, _, _) -> executor.invoke((Sender) v0));
    }
    @Override
    default Self withExecutor(Func0<Boolean> executor) {
        return withExecutor((_, _, _) -> executor.invoke());
    }
}
