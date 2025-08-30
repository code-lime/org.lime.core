package org.lime.core.common.utils;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

public class AnnotationUtils {
    public record Info<T>(T annotation, AnnotatedElement member, Type target) {
        public static <T> Info<T> of(T annotation, AnnotatedElement member, Type target) {
            return new Info<>(annotation, member, target);
        }
    }

    public static <T extends Annotation> Stream<Info<T>> recursiveAnnotations(
            Class<T> annotationClass,
            Type targetType) {
        Class<?> raw = TypeUtils.getRawType(targetType, targetType);
        if (raw == null)
            return Stream.empty();

        Map<TypeVariable<?>, Type> typeVarAssigns = Optional.ofNullable(TypeUtils.getTypeArguments(targetType, raw))
                .orElse(Collections.emptyMap());

        Type resolvedTargetType = TypeUtils.unrollVariables(typeVarAssigns, targetType);

        Stream<Info<T>> classLevel = Arrays.stream(raw.getDeclaredAnnotationsByType(annotationClass))
                .map(a -> Info.of(a, raw, resolvedTargetType));

        Stream<Info<T>> fields = Arrays.stream(raw.getDeclaredFields())
                .flatMap(f -> recursiveAnnotations(annotationClass, f, typeVarAssigns));

        Stream<Info<T>> constructors = Arrays.stream(raw.getDeclaredConstructors())
                .flatMap(c -> recursiveAnnotations(annotationClass, c, typeVarAssigns));

        Stream<Info<T>> methods = Arrays.stream(raw.getDeclaredMethods())
                .flatMap(m -> recursiveAnnotations(annotationClass, m, typeVarAssigns));

        return Streams.concat(classLevel, fields, constructors, methods);
    }

    private static <T extends Annotation> Stream<Info<T>> recursiveAnnotations(
            Class<T> annotationClass,
            Field field,
            Map<TypeVariable<?>, Type> typeVarAssigns) {
        Type fieldGeneric = field.getGenericType();
        Type resolvedFieldType = TypeUtils.unrollVariables(typeVarAssigns, fieldGeneric);

        Stream<Info<T>> annotationsOnField = Arrays.stream(field.getDeclaredAnnotationsByType(annotationClass))
                .map(a -> Info.of(a, field, resolvedFieldType));
        Stream<Info<T>> annotationsOnFieldType = Arrays.stream(field.getAnnotatedType().getDeclaredAnnotationsByType(annotationClass))
                .map(a -> Info.of(a, field, resolvedFieldType));
        Stream<Info<T>> inner = recursiveAnnotationsOnInnerType(annotationClass, resolvedFieldType, typeVarAssigns);
        return Streams.concat(annotationsOnField, annotationsOnFieldType, inner);
    }
    private static <T extends Annotation> Stream<Info<T>> recursiveAnnotations(
            Class<T> annotationClass,
            Method method,
            Map<TypeVariable<?>, Type> ownerTypeVarAssigns) {
        Stream<Info<T>> annotationsOnMethod = Arrays.stream(method.getDeclaredAnnotationsByType(annotationClass))
                .map(a -> Info.of(a, method, TypeUtils.unrollVariables(ownerTypeVarAssigns, method.getDeclaringClass())));

        Type returnGeneric = method.getGenericReturnType();
        Type resolvedReturnType = TypeUtils.unrollVariables(ownerTypeVarAssigns, returnGeneric);

        Stream<Info<T>> annotationsOnReturnType = Arrays.stream(method.getAnnotatedReturnType().getDeclaredAnnotationsByType(annotationClass))
                .map(a -> Info.of(a, method, resolvedReturnType));
        Stream<Info<T>> params = recursiveAnnotations(annotationClass, (Executable) method, ownerTypeVarAssigns);
        Stream<Info<T>> inner = recursiveAnnotationsOnInnerType(annotationClass, resolvedReturnType, ownerTypeVarAssigns);

        return Streams.concat(annotationsOnMethod, annotationsOnReturnType, params, inner);
    }
    private static <T extends Annotation> Stream<Info<T>> recursiveAnnotations(
            Class<T> annotationClass,
            Constructor<?> constructor,
            Map<TypeVariable<?>, Type> ownerTypeVarAssigns) {
        Stream<Info<T>> annotationsOnCtor = Arrays.stream(constructor.getDeclaredAnnotationsByType(annotationClass))
                .map(a -> Info.of(a, constructor, TypeUtils.unrollVariables(ownerTypeVarAssigns, constructor.getDeclaringClass())));

        Stream<Info<T>> params = recursiveAnnotations(annotationClass, (Executable) constructor, ownerTypeVarAssigns);

        return Streams.concat(annotationsOnCtor, params);
    }
    private static <T extends Annotation> Stream<Info<T>> recursiveAnnotations(
            Class<T> annotationClass,
            Executable executable,
            Map<TypeVariable<?>, Type> ownerTypeVarAssigns) {
        Map<TypeVariable<?>, Type> baseAssigns = new HashMap<>(ownerTypeVarAssigns);

        return Arrays.stream(executable.getParameters())
                .flatMap(parameter -> {
                    Type paramType = parameter.getParameterizedType();
                    Type resolved = TypeUtils.unrollVariables(baseAssigns, paramType);

                    Stream<Info<T>> annotationsOnParam = Arrays.stream(parameter.getDeclaredAnnotationsByType(annotationClass))
                            .map(a -> Info.of(a, executable, resolved));

                    Stream<Info<T>> annotationsOnParamType = Arrays.stream(parameter.getAnnotatedType().getDeclaredAnnotationsByType(annotationClass))
                            .map(a -> Info.of(a, executable, resolved));

                    Stream<Info<T>> inner = recursiveAnnotationsOnInnerType(annotationClass, resolved, baseAssigns);

                    return Streams.concat(annotationsOnParam, annotationsOnParamType, inner);
                });
    }
    private static <T extends Annotation> Stream<Info<T>> recursiveAnnotationsOnInnerType(
            Class<T> annotationClass,
            Type resolvedType,
            Map<TypeVariable<?>, Type> parentAssigns) {
        Class<?> raw = TypeUtils.getRawType(resolvedType, resolvedType);
        if (raw == null)
            return Stream.empty();

        Map<TypeVariable<?>, Type> newAssigns = Optional.ofNullable(TypeUtils.getTypeArguments(resolvedType, raw))
                .orElse(Collections.emptyMap());

        Map<TypeVariable<?>, Type> mergedAssigns = new HashMap<>(parentAssigns);
        mergedAssigns.putAll(newAssigns);

        Stream<Info<T>> fields = Arrays.stream(raw.getDeclaredFields())
                .flatMap(f -> recursiveAnnotations(annotationClass, f, mergedAssigns));

        Stream<Info<T>> constructors = Arrays.stream(raw.getDeclaredConstructors())
                .flatMap(c -> recursiveAnnotations(annotationClass, c, mergedAssigns));

        Stream<Info<T>> methods = Arrays.stream(raw.getDeclaredMethods())
                .flatMap(m -> recursiveAnnotations(annotationClass, m, mergedAssigns));

        return Streams.concat(fields, constructors, methods);
    }

    public static Class<?> findClassWithAnnotation(
            Class<?> type,
            Class<? extends Annotation> annotation) {
        Deque<Class<?>> toVisit = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();

        toVisit.add(type);

        while (!toVisit.isEmpty()) {
            Class<?> current = toVisit.poll();

            if (current == null || current == Object.class || !visited.add(current))
                continue;

            if (current.getDeclaredAnnotationsByType(annotation).length > 0)
                return current;

            var superClass = current.getSuperclass();
            if (superClass != null)
                toVisit.add(superClass);
            Collections.addAll(toVisit, current.getInterfaces());
        }

        return null;
    }
}
