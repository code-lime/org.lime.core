package org.lime.core.common.reflection;

import java.lang.reflect.Member;

public interface ReflectionMember<T extends Member, Self extends ReflectionMember<T, Self>>
        extends ReflectionBase<T, Self> {
    default boolean is(int... modifiers) {
        int targetMod = target().getModifiers();
        for (int mod : modifiers)
            if ((targetMod & mod) == 0)
                return false;
        return true;
    }
}
