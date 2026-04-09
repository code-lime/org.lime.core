package org.lime.core.common.services.buffers;

public @interface InjectBuffer {
    String tag();
    int trackingDistance() default -1;
}
