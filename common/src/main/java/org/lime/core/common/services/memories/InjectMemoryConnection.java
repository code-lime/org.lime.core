package org.lime.core.common.services.memories;

public @interface InjectMemoryConnection {
    String UNIQUE_KEY = "";

    String key() default UNIQUE_KEY;
}
