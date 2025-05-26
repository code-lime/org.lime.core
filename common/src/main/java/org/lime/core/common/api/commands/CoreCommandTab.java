package org.lime.core.common.api.commands;

import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.execute.Func2;

import java.util.Arrays;
import java.util.Collection;

public interface CoreCommandTab<Sender, Data, Self extends CoreCommandTab<Sender, Data, Self>>
        extends CoreCommandTabSimple<Self> {
    Self withTab(CommandAction<Sender, Data, Collection<String>> tab);

    @Override
    default Self withTabSimple(Func1<String[], Collection<String>> tab) {
        return withTab((v0, args) -> tab.invoke(args));
    }

    default Self withTab(Func2<Sender, String[], Collection<String>> tab) {
        return withTab((v0, v1, v3) -> tab.invoke(v0, v3));
    }
    default Self withTab(Func1<Sender, Collection<String>> tab) {
        return withTab((v0, v1, v3) -> tab.invoke(v0));
    }

    @Override
    default Self withTab(Func0<Collection<String>> tab) {
        return withTab((v0, v1, v3) -> tab.invoke());
    }
    @Override
    default Self withTab(Collection<String> tab) {
        return withTab((v0, v1, v3) -> tab);
    }
    @Override
    default Self withTab(String... tab) {
        return withTab(Arrays.asList(tab));
    }
}
