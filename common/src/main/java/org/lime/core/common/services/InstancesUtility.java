package org.lime.core.common.services;

import org.lime.core.common.BaseInstance;

import java.util.stream.Stream;

public interface InstancesUtility {
    Stream<? extends BaseInstance<?>> instances();
}
