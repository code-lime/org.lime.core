package org.lime.core.common.api.elements;

public interface CoreElementIdentity<T> {
    String name();
    Class<T> elementClass();
}
