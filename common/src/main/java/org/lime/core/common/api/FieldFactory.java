package org.lime.core.common.api;

import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

public interface FieldFactory {
    <I> boolean validate(TypeLiteral<I> declareType, Field field, TypeEncounter<I> encounter);
    <I, T> @NotNull Function<Disposable.Composite, T> create(TypeLiteral<I> declareType, TypeLiteral<T> fieldType, ReflectionField<T> field, TypeEncounter<I> encounter);

    @SuppressWarnings({"rawtypes", "unchecked"})
    default <I, T> MembersInjector<I> createInjector(
            TypeLiteral<I> declareType,
            ReflectionField<T> field,
            TypeEncounter<I> encounter,
            Disposable.Composite compositeDisposable) {
        var rawField = field.target();
        Function<Disposable.Composite, T> provider = create(declareType, (TypeLiteral<T>) declareType.getFieldType(rawField), field, encounter);

        if (field.is(Modifier.STATIC)) {
            Action1 staticSetter = field.setter(Action1.class);
            return instance -> staticSetter.invoke(provider.apply(compositeDisposable));
        } else {
            Action2 instanceSetter = field.setter(Action2.class);
            return instance -> instanceSetter.invoke(instance, provider.apply(compositeDisposable));
        }
    }
    default <I>boolean register(TypeLiteral<I> declareType, Field field, TypeEncounter<I> encounter, Disposable.Composite compositeDisposable) {
        if (!validate(declareType, field, encounter))
            return false;
        encounter.register(createInjector(declareType, ReflectionField.of(field), encounter, compositeDisposable));
        return true;
    }

    default FieldFactory annotated(Class<? extends Annotation> annotation) {
        var owner = this;
        return new FieldFactory() {
            @Override
            public <I> boolean validate(TypeLiteral<I> declareType, Field field, TypeEncounter<I> encounter) {
                return field.isAnnotationPresent(annotation) && owner.validate(declareType, field, encounter);
            }
            @Override
            public <I, T> @NotNull Function<Disposable.Composite, T> create(TypeLiteral<I> declareType, TypeLiteral<T> fieldType, ReflectionField<T> field, TypeEncounter<I> encounter) {
                return owner.create(declareType, fieldType, field, encounter);
            }
        };
    }

    abstract class Annotated<A extends Annotation>
            implements FieldFactory {
        protected final Class<A> annotationClass;

        public Annotated(Class<A> annotationClass) {
            Reflection.validateRuntimeAnnotation(annotationClass);
            this.annotationClass = annotationClass;
        }

        protected <I> boolean validate(TypeLiteral<I> declareType, Field field, A annotation, TypeEncounter<I> encounter) {
            return true;
        }
        protected abstract <I, T> @NotNull Function<Disposable.Composite, T> create(TypeLiteral<I> declareType, TypeLiteral<T> fieldType, ReflectionField<T> field, A annotation, TypeEncounter<I> encounter);

        @Override
        public <I> boolean validate(TypeLiteral<I> declareType, Field field, TypeEncounter<I> encounter) {
            var annotation = field.getAnnotation(annotationClass);
            return annotation != null && validate(declareType, field, annotation, encounter);
        }
        @Override
        public <I, T> @NotNull Function<Disposable.Composite, T> create(TypeLiteral<I> declareType, TypeLiteral<T> fieldType, ReflectionField<T> field, TypeEncounter<I> encounter) {
            return create(declareType, fieldType, field, field.target().getAnnotation(annotationClass), encounter);
        }

        @Override
        public String toString() {
            return "@" + annotationClass.getName();
        }
    }
    abstract class AnnotatedGeneric<A extends Annotation>
            extends Annotated<A> {
        private final Class<?> genericClass;

        public AnnotatedGeneric(Class<?> genericClass, Class<A> annotationClass) {
            super(annotationClass);
            this.genericClass = genericClass;
        }

        protected abstract <T>Function<Disposable.Composite, T> create(A annotation, TypeLiteral<?> key, TypeEncounter<?> encounter);

        @Override
        public @NotNull <I, T> Function<Disposable.Composite, T> create(
                TypeLiteral<I> declareType,
                TypeLiteral<T> fieldType,
                ReflectionField<T> field,
                A annotation,
                TypeEncounter<I> encounter) {
            if (fieldType.getRawType() != genericClass)
                throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not '"+genericClass+"'");
            Type[] args;
            if (!(fieldType.getType() instanceof ParameterizedType parameterizedType) || (args = parameterizedType.getActualTypeArguments()).length != 1)
                throw new IllegalArgumentException("In field '"+field+"' return type '"+fieldType+"' is not parameterized '"+genericClass+"'");
            var key = TypeLiteral.get(args[0]);
            return create(annotation, key, encounter);
        }
    }
}
