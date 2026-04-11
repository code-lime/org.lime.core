package org.lime.core.common.services.memories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectMemoryConnection {
    String UNIQUE_KEY = "";

    String key() default UNIQUE_KEY;
}
