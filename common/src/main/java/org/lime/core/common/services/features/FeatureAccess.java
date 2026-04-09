package org.lime.core.common.services.features;

import org.lime.core.common.api.ConfigAccess;
import org.lime.core.common.utils.execute.Action1;

public interface FeatureAccess<T> {
    T get();
    void modify(Action1<T> modify);

    default <V extends Enum<V> & FeatureType<V>>FeatureAccessField<V> as(
            Class<V> valueClass,
            FeatureField<T, V> field) {
        return FeatureAccessField.of(this, field, valueClass);
    }
    default FeatureAccessField<FeatureType.BiType> asBoolean(
            FeatureField<T, Boolean> field) {
        return as(FeatureType.BiType.class, field.map(FeatureType.BiType.CONVERTER));
    }

    static <T>FeatureAccess<T> ofInstance(T instance) {
        return new FeatureAccess<>() {
            @Override
            public T get() {
                return instance;
            }
            @Override
            public void modify(Action1<T> modify) {
                modify.invoke(instance);
            }
        };
    }
    static <T>FeatureAccess<T> ofConfig(ConfigAccess<T> config) {
        return new FeatureAccess<>() {
            @Override
            public T get() {
                return config.value();
            }
            @Override
            public void modify(Action1<T> modify) {
                var value = config.value();
                modify.invoke(value);
                config.save(value);
            }
        };
    }
}
