package org.lime.core.common.utils.typeadapers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonCastProperty {
    String value() default "type";
}
