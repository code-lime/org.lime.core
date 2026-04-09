package org.lime.core.common.services.cooldowns;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectCooldown {
    String UNIQUE_GROUP = "";

    String group() default UNIQUE_GROUP;
    long defaultCooldownMillis() default 0;
}
