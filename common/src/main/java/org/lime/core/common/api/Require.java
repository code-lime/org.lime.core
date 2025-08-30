package org.lime.core.common.api;

import java.lang.annotation.*;

@Repeatable(Requires.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Require {
    Class<?> value();
}

