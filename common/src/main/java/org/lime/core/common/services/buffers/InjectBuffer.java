package org.lime.core.common.services.buffers;

import net.kyori.adventure.key.KeyPattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectBuffer {
    String tag();
    @KeyPattern String entityKey() default "";
    int trackingDistance() default -1;
}
