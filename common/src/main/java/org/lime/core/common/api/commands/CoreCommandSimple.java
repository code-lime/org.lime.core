package org.lime.core.common.api.commands;

public interface CoreCommandSimple<Self extends CoreCommandSimple<Self>>
        extends CoreCommandCheckSimple<Self>, CoreCommandExecutorSimple<Self>, CoreCommandTabSimple<Self> {
}
