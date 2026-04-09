package org.lime.core.common.services.features;

import org.lime.core.common.api.commands.CommandConsumer;

public interface FeatureAccessField<V extends Enum<V> & FeatureType<V>> {
    V get();
    void set(V value);

    Class<V> valueClass();

    default CommandConsumer<?> build(
            String key,
            String name,
            FeaturesHelperService service) {
        return service.createFeatureCommand(key, name, this);
    }

    static <T, V extends Enum<V> & FeatureType<V>>FeatureAccessField<V> of(
            FeatureAccess<T> access,
            FeatureField<T, V> field,
            Class<V> valueClass) {
        return new FeatureAccessField<>() {
            @Override
            public V get() {
                return field.get(access.get());
            }
            @Override
            public void set(V value) {
                access.modify(j -> field.set(j, value));
            }
            @Override
            public Class<V> valueClass() {
                return valueClass;
            }
        };
    }
}
