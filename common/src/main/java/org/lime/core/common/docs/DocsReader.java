package org.lime.core.common.docs;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class DocsReader {
    public static <A, B extends A> List<IIndexGroup> groups(Class<A> aClass, B data, boolean checkParent) {
        try {
            Class<?> bClass = data.getClass();
            List<IIndexGroup> groups = new ArrayList<>();
            for (Method aMethod : aClass.getDeclaredMethods()) {
                if (Modifier.isStatic(aMethod.getModifiers())) continue;
                if (aMethod.getReturnType() != IIndexDocs.class) continue;
                if (aMethod.getParameterCount() > 0) continue;
                Method bMethod = bClass.getMethod(aMethod.getName());
                if (!(bMethod.invoke(data) instanceof IIndexGroup group)) continue;
                if (checkParent && group.parent() != null) continue;
                groups.add(group);
            }
            return groups;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
