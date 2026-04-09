package org.lime.core.common.services.features;

import com.google.common.base.Converter;

public interface FeatureType<T extends Enum<T> & FeatureType<T>> {
    enum BiType
            implements FeatureType<BiType> {
        ENABLE(true),
        DISABLE(false);

        public static final Converter<BiType, Boolean> CONVERTER = Converter.from(BiType::enable, BiType::ofBoolean);

        public final boolean enable;

        BiType(boolean enable) {
            this.enable = enable;
        }

        @Override
        public boolean enable() {
            return enable;
        }

        public static BiType ofBoolean(boolean enable) {
            return enable ? ENABLE : DISABLE;
        }
    }

    boolean enable();
}
