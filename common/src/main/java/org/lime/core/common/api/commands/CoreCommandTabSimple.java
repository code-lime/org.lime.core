package org.lime.core.common.api.commands;

import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;

import java.util.Collection;

public interface CoreCommandTabSimple<Self extends CoreCommandTabSimple<Self>> {
    Self withTabSimple(Func1<String[], Collection<String>> tab);
    Self withTab(Func0<Collection<String>> tab);
    Self withTab(Collection<String> tab);
    Self withTab(String... tab);
}
