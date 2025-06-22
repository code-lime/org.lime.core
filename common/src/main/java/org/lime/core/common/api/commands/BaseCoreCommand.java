package org.lime.core.common.api.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.BaseLogger;
import org.lime.core.common.system.execute.Action1;
import org.lime.core.common.system.execute.Func2;
import org.lime.core.common.system.utils.IterableUtils;

import java.util.Collection;
import java.util.Objects;

public abstract class BaseCoreCommand<Sender, Data, Self extends BaseCoreCommand<Sender, Data, Self>>
        implements CoreCommandTab<Sender, Data, Self>, CoreCommandCheck<Sender, Data, Self>, CoreCommandExecutor<Sender, Data, Self>, CoreCommandSimple<Self> {
    public final String cmd;
    protected final Class<Sender> sender;
    protected @Nullable String description = null;
    protected @Nullable String usage = null;
    protected @Nullable CommandAction<Sender, Data, Boolean> check = null;
    protected @Nullable CommandAction<Sender, Data, Collection<String>> tab = null;
    protected @Nullable CommandAction<Sender, Data, Boolean> executor = null;
    protected @Nullable Action1<ArgumentBuilder<Data, ?>> nativeCommand = null;

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
    public Self withNative(Action1<ArgumentBuilder<Data, ?>> nativeCommand) {
        this.nativeCommand = nativeCommand;
        return self();
    }

    public Self join(Self next, BaseLogger logger) {
        if ((this.nativeCommand == null) != (next.nativeCommand == null))
            logger.$logOP("Warning: Join commands with label '"+this.cmd+"' is maybe override by native executor");
        this.description = combine(this.description, next.description, (a,b) -> a.equalsIgnoreCase(b) ? a : (a + " and " + b));
        this.usage = combine(this.usage, next.usage, (a,b) -> a.equalsIgnoreCase(b) ? a : (a + " or " + b));
        this.check = combineNullable(this.check, next.check);
        this.tab = combine(this.tab, next.tab, (a,b) -> (v0,v1,v2) -> IterableUtils.concat(a.action(v0,v1,v2), b.action(v0,v1,v2)));
        this.executor = combineNullable(this.executor, next.executor);
        this.nativeCommand = combine(this.nativeCommand, next.nativeCommand, Action1::andThen);
        return self();
    }

    protected static <Sender, Data>@NotNull CommandAction<Sender, Data, Boolean> combine(@Nullable CommandAction<Sender, Data, Boolean> executor1, @Nullable CommandAction<Sender, Data, Boolean> executor2) {
        return Objects.requireNonNullElseGet(combineNullable(executor1, executor2), () -> (v0, v1, v3) -> true);
    }
    private static <Sender, Data>@Nullable CommandAction<Sender, Data, Boolean> combineNullable(@Nullable CommandAction<Sender, Data, Boolean> executor1, @Nullable CommandAction<Sender, Data, Boolean> executor2) {
        return combine(executor1, executor2, (a,b) -> (v0, v1, v3) -> a.action(v0, v1, v3) && b.action(v0, v1, v3));
    }
    private static <T>@Nullable T combine(@Nullable T a, @Nullable T b, Func2<T, T, T> combine) {
        return a != null
                ? (b != null ? combine.invoke(a,b) : a)
                : b;
    }
}
