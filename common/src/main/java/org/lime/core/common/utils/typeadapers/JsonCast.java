package org.lime.core.common.utils.typeadapers;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(JsonCasts.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonCast {
    String name();
    Class<?> type();
}
