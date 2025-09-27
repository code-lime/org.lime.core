package org.lime.core.common.services;

import org.lime.core.common.BaseInstance;

import java.util.stream.Stream;

public interface InstancesUtility {
    BaseInstance<?> core();
    Stream<? extends BaseInstance<?>> instances();
}
