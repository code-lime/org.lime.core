package org.lime.core.common.api.commands;

import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;

public interface CoreCommandCheckSimple<Self extends CoreCommandCheckSimple<Self>> {
    Self addCheckSimple(Func1<String[], Boolean> tab);
    Self addOperatorOnly();
    Self addCheck(Func0<Boolean> check);
    Self addCheck(String... permissions);
}
