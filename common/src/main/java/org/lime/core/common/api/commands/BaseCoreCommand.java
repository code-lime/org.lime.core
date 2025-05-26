package org.lime.core.common.api.commands;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class BaseCoreCommand<Sender, Data, Self extends BaseCoreCommand<Sender, Data, Self>>
        implements CoreCommandTab<Sender, Data, Self>, CoreCommandCheck<Sender, Data, Self>, CoreCommandExecutor<Sender, Data, Self>, CoreCommandSimple<Self> {
    public final String cmd;
    protected Class<Sender> sender;
    protected @Nullable String description = null;
    protected @Nullable String usage = null;
    protected @Nullable CommandAction<Sender, Data, Boolean> check = null;
    protected @Nullable CommandAction<Sender, Data, Collection<String>> tab = null;
    protected @Nullable CommandAction<Sender, Data, Boolean> executor = null;

    public String cmd() {
        return cmd;
    }

    protected BaseCoreCommand(String cmd, Class<Sender> sender) {
        this.cmd = cmd;
        this.sender = sender;
    }

    protected abstract Self self();

    @Override
    public Self addCheck(CommandAction<Sender, Data, Boolean> check) {
        this.check = combine(this.check, check);
        return self();
    }
    @Override
    public Self withTab(CommandAction<Sender, Data, Collection<String>> tab) {
        this.tab = tab;
        return self();
    }
    @Override
    public Self withExecutor(CommandAction<Sender, Data, Boolean> executor) {
        this.executor = executor;
        return self();
    }

    public Self withDescription(String description) {
        this.description = description;
        return self();
    }
    public Self withUsage(String usage) {
        this.usage = usage;
        return self();
    }

    protected static <Sender, Data>CommandAction<Sender, Data, Boolean> combine(CommandAction<Sender, Data, Boolean> executor1, CommandAction<Sender, Data, Boolean> executor2) {
        return executor1 == null
                ? (executor2 == null ? (v0, v1, v3) -> true : executor2)
                : (executor2 == null ? executor1 : (v0, v1, v3) -> executor1.action(v0, v1, v3) && executor2.action(v0, v1, v3));
    }
}
