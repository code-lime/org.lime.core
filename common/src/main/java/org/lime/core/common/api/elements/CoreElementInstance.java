package org.lime.core.common.api.elements;

public interface CoreElementInstance<T, Self extends CoreElementInstance<T, Self>> {
    Class<T> elementClass();

    Self withInstance(T instance);

    default Self withInstance() {
        try {
            return withInstance(elementClass().getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
