package org.lime.core.common.services.features;

import com.google.common.base.Converter;
import org.lime.core.common.reflection.Lambda;
import org.lime.core.common.reflection.LambdaInfo;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;

public interface FeatureField<T, J> {
    void set(T instance, J value);
    J get(T instance);

    default <I>FeatureField<T, I> map(Converter<I, J> converter) {
        var reverse = converter.reverse();
        var owner = this;
        return new FeatureField<>() {
            @Override
            public void set(T instance, I value) {
                 owner.set(instance, converter.convert(value));
            }
            @Override
            public I get(T instance) {
                return reverse.convert(owner.get(instance));
            }
        };
    }

    static <T, J>FeatureField<T, J> ofLambda(Func1<T, J> lambda) {
        var fieldInfo = LambdaInfo.getField(lambda);

        Action2<T, J> setter = Lambda.setter(fieldInfo, Action2.class);
        Func1<T, J> getter = Lambda.getter(fieldInfo, Func1.class);

        return new FeatureField<>() {
            @Override
            public void set(T instance, J value) {
                setter.invoke(instance, value);
            }
            @Override
            public J get(T instance) {
                return getter.invoke(instance);
            }
        };
    }
}
