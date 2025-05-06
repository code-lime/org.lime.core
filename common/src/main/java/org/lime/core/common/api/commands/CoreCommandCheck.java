package org.lime.core.common.api.commands;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointered;
import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.execute.Func2;

public interface CoreCommandCheck<Sender, Data, Self extends CoreCommandCheck<Sender, Data, Self>>
        extends CoreCommandCheckSimple<Self> {
    Self addCheck(CommandAction<Sender, Data, Boolean> check);

    @Override
    default Self addCheckSimple(Func1<String[], Boolean> check) {
        return addCheck((_,args) -> check.invoke(args));
    }
    @Override
    default Self addOperatorOnly() {
        return addCheck(operator());
    }
    default Self addCheck(Func2<Sender, String[], Boolean> check) {
        return addCheck((v0, _, v3) -> check.invoke((Sender)v0, v3));
    }
    default Self addCheck(Func1<Sender, Boolean> check) {
        return addCheck((v0, _, _) -> check.invoke((Sender)v0));
    }
    @Override
    default Self addCheck(Func0<Boolean> check) {
        return addCheck((_, _, _) -> check.invoke());
    }
    @Override
    default Self addCheck(String... permissions) {
        return addCheck((s, _, _) -> {
            if (!(s instanceof Pointered pointered))
                return false;
            PermissionChecker checker = pointered.get(PermissionChecker.POINTER).orElse(null);
            if (checker == null)
                return false;
            for (String perm : permissions)
                if (checker.test(perm))
                    return true;
            return false;
        });
    }

    default <I extends Sender, J extends BaseCoreCommand<I, Data, J>>J addCheckCast(Class<I> sender) {
        return (J)addCheck(sender::isInstance);
    }

    CommandAction<Sender, Data, Boolean> operator();
}
