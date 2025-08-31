package org.lime.core.common.utils.adapters;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonCastProperty {
    String value() default "type";
}
