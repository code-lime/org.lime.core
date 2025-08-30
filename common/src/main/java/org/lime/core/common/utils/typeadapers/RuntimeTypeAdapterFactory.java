package org.lime.core.common.utils.typeadapers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.AnnotationUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapts values whose runtime type may differ from their declaration type. This is necessary when a
 * field's type is not the same type that GSON should create when deserializing that field. For
 * example, consider these types:
 *
 * <pre>{@code
 * abstract class Shape {
 *   int x;
 *   int y;
 * }
 * class Circle extends Shape {
 *   int radius;
 * }
 * class Rectangle extends Shape {
 *   int width;
 *   int height;
 * }
 * class Diamond extends Shape {
 *   int width;
 *   int height;
 * }
 * class Drawing {
 *   Shape bottomShape;
 *   Shape topShape;
 * }
 * }</pre>
 *
 * <p>Without additional type information, the serialized JSON is ambiguous. Is the bottom shape in
 * this drawing a rectangle or a diamond?
 *
 * <pre>{@code
 * {
 *   "bottomShape": {
 *     "width": 10,
 *     "height": 5,
 *     "x": 0,
 *     "y": 0
 *   },
 *   "topShape": {
 *     "radius": 2,
 *     "x": 4,
 *     "y": 1
 *   }
 * }
 * }</pre>
 *
 * This class addresses this problem by adding type information to the serialized JSON and honoring
 * that type information when the JSON is deserialized:
 *
 * <pre>{@code
 * {
 *   "bottomShape": {
 *     "type": "Diamond",
 *     "width": 10,
 *     "height": 5,
 *     "x": 0,
 *     "y": 0
 *   },
 *   "topShape": {
 *     "type": "Circle",
 *     "radius": 2,
 *     "x": 4,
 *     "y": 1
 *   }
 * }
 * }</pre>
 *
 * Both the type field name ({@code "type"}) and the type labels ({@code "Rectangle"}) are
 * configurable.
 *
 * <h2>Registering Types</h2>
 *
 * Create a {@code RuntimeTypeAdapterFactory} by passing the base type and type field name to the
 * {@link #of} factory method. If you don't supply an explicit type field name, {@code "type"} will
 * be used.
 *
 * <pre>{@code
 * RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory
 *     = RuntimeTypeAdapterFactory.of(Shape.class, "type");
 * }</pre>
 *
 * Next register all of your subtypes. Every subtype must be explicitly registered. This protects
 * your application from injection attacks. If you don't supply an explicit type label, the type's
 * simple name will be used.
 *
 * <pre>{@code
 * shapeAdapterFactory.registerSubtype(Rectangle.class, "Rectangle");
 * shapeAdapterFactory.registerSubtype(Circle.class, "Circle");
 * shapeAdapterFactory.registerSubtype(Diamond.class, "Diamond");
 * }</pre>
 *
 * Finally, register the type adapter factory in your application's GSON builder:
 *
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapterFactory(shapeAdapterFactory)
 *     .create();
 * }</pre>
 *
 * Like {@code GsonBuilder}, this API supports chaining:
 *
 * <pre>{@code
 * RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Shape.class)
 *     .registerSubtype(Rectangle.class)
 *     .registerSubtype(Circle.class)
 *     .registerSubtype(Diamond.class);
 * }</pre>
 *
 * <h2>Serialization and deserialization</h2>
 *
 * In order to serialize and deserialize a polymorphic object, you must specify the base type
 * explicitly.
 *
 * <pre>{@code
 * Diamond diamond = new Diamond();
 * String json = gson.toJson(diamond, Shape.class);
 * }</pre>
 *
 * And then:
 *
 * <pre>{@code
 * Shape shape = gson.fromJson(json, Shape.class);
 * }</pre>
 */
public final class RuntimeTypeAdapterFactory<T>
        implements TypeAdapterFactory {
    private static final String DEFAULT_TYPE_FIELD_NAME = "type";

    private final Class<T> baseType;
    private final String typeFieldName;
    private final Map<String, Class<? extends T>> labelToSubtype = new LinkedHashMap<>();
    private final Map<Class<? extends T>, String> subtypeToLabel = new LinkedHashMap<>();
    private final boolean maintainType;
    private boolean recognizeSubtypes;

    private RuntimeTypeAdapterFactory(Class<T> baseType, @Nullable String typeFieldName, boolean maintainType) {
        if (baseType == null) {
            throw new NullPointerException("baseType");
        }

        @Nullable JsonCastProperty propertyInfo = baseType.getDeclaredAnnotation(JsonCastProperty.class);

        this.baseType = baseType;
        if (typeFieldName == null) {
            typeFieldName = propertyInfo == null
                    ? DEFAULT_TYPE_FIELD_NAME
                    : propertyInfo.value();
        } else if (propertyInfo == null) {
            throw new IllegalArgumentException("typeFieldName is not null & annotation JsonCastProperty exist");
        }

        this.typeFieldName = typeFieldName;
        this.maintainType = maintainType;

        for (JsonCast cast : baseType.getDeclaredAnnotationsByType(JsonCast.class)) {
            Class<?> castType = cast.type();
            if (baseType.isAssignableFrom(castType)) {
                //noinspection unchecked
                registerSubtype((Class<? extends T>) castType, cast.name());
            } else {
                throw new ClassCastException("Cannot cast " + castType.getName() + " to " + baseType.getName());
            }
        }
    }

    /**
     * Creates a new runtime type adapter for {@code baseType} using {@code typeFieldName} as the type
     * field name. Type field names are case-sensitive.
     *
     * @param maintainType true if the type field should be included in deserialized objects
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(
            Class<T> baseType, String typeFieldName, boolean maintainType) {
        return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName, maintainType);
    }

    /**
     * Creates a new runtime type adapter for {@code baseType} using {@code typeFieldName} as the type
     * field name. Type field names are case sensitive.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName, false);
    }

    /**
     * Creates a new runtime type adapter for {@code baseType} using {@code "type"} as the type field
     * name.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType) {
        return new RuntimeTypeAdapterFactory<>(baseType, null, false);
    }

    /**
     * Creates a new runtime type adapter for {@code baseType} using {@code "type"} as the type field
     * name.
     *
     * @param maintainType true if the type field should be included in deserialized objects
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, boolean maintainType) {
        return new RuntimeTypeAdapterFactory<>(baseType, null, maintainType);
    }

    /**
     * Ensures that this factory will handle not just the given {@code baseType}, but any subtype of
     * that type.
     */
    @CanIgnoreReturnValue
    public RuntimeTypeAdapterFactory<T> recognizeSubtypes() {
        this.recognizeSubtypes = true;
        return this;
    }

    /**
     * Registers {@code type} identified by {@code label}. Labels are case-sensitive.
     *
     * @throws IllegalArgumentException if either {@code type} or {@code label} have already been
     *     registered on this type adapter.
     */
    @CanIgnoreReturnValue
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
        if (type == null || label == null) {
            throw new NullPointerException();
        }
        if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
            throw new IllegalArgumentException("types and labels must be unique");
        }
        labelToSubtype.put(label, type);
        subtypeToLabel.put(type, label);
        return this;
    }

    /**
     * Registers {@code type} identified by its {@link Class#getSimpleName simple name}. Labels are
     * case-sensitive.
     *
     * @throws IllegalArgumentException if either {@code type} or its simple name have already been
     *     registered on this type adapter.
     */
    @CanIgnoreReturnValue
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type) {
        return registerSubtype(type, type.getSimpleName());
    }

    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
        if (type == null) {
            return null;
        }
        Class<?> rawType = type.getRawType();
        boolean handle =
                recognizeSubtypes ? baseType.isAssignableFrom(rawType) : baseType.equals(rawType);
        if (!handle) {
            return null;
        }

        TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        Map<String, TypeAdapter<?>> labelToDelegate = new LinkedHashMap<>();
        Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();
        for (Map.Entry<String, Class<? extends T>> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            labelToDelegate.put(entry.getKey(), delegate);
            subtypeToDelegate.put(entry.getValue(), delegate);
        }

        return new TypeAdapter<R>() {
            @Override
            public R read(JsonReader in) throws IOException {
                JsonElement jsonElement = jsonElementAdapter.read(in);
                JsonElement labelJsonElement;
                if (maintainType) {
                    labelJsonElement = jsonElement.getAsJsonObject().get(typeFieldName);
                } else {
                    labelJsonElement = jsonElement.getAsJsonObject().remove(typeFieldName);
                }

                if (labelJsonElement == null) {
                    throw new JsonParseException(
                            "cannot deserialize "
                                    + baseType
                                    + " because it does not define a field named "
                                    + typeFieldName);
                }
                String label = labelJsonElement.getAsString();
                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
                if (delegate == null) {
                    throw new JsonParseException(
                            "cannot deserialize "
                                    + baseType
                                    + " subtype named "
                                    + label
                                    + "; did you forget to register a subtype?");
                }
                return delegate.fromJsonTree(jsonElement);
            }

            @Override
            public void write(JsonWriter out, R value) throws IOException {
                Class<?> srcType = value.getClass();
                String label = subtypeToLabel.get(srcType);
                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
                if (delegate == null) {
                    throw new JsonParseException(
                            "cannot serialize " + srcType.getName() + "; did you forget to register a subtype?");
                }
                JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();

                if (maintainType) {
                    jsonElementAdapter.write(out, jsonObject);
                    return;
                }

                JsonObject clone = new JsonObject();

                if (jsonObject.has(typeFieldName)) {
                    throw new JsonParseException(
                            "cannot serialize "
                                    + srcType.getName()
                                    + " because it already defines a field named "
                                    + typeFieldName);
                }
                clone.add(typeFieldName, new JsonPrimitive(label));

                for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                    clone.add(e.getKey(), e.getValue());
                }
                jsonElementAdapter.write(out, clone);
            }
        }.nullSafe();
    }

    public static final TypeAdapterFactory AUTO = new TypeAdapterFactory() {
        private final ConcurrentHashMap<Class<?>, Optional<TypeAdapterFactory>> classes = new ConcurrentHashMap<>();

        private Optional<TypeAdapterFactory> getFactory(Class<?> tClass) {
            List<Class<?>> other = new ArrayList<>();
            Optional<TypeAdapterFactory> result = classes.computeIfAbsent(tClass, v -> {
                Class<?> annotatedClass = AnnotationUtils.findClassWithAnnotation(v, JsonCast.class);
                if (annotatedClass == null)
                    return Optional.empty();
                if (!annotatedClass.equals(v))
                    other.add(annotatedClass);
                return Optional.of(new RuntimeTypeAdapterFactory<>(annotatedClass, null, false));
            });
            other.forEach(annotatedClass -> classes.put(annotatedClass, result));
            return result;
        }

        @Override
        public <J> TypeAdapter<J> create(Gson gson, TypeToken<J> type) {
            return getFactory(type.getRawType())
                    .map(v -> v.create(gson, type))
                    .orElse(null);
        }
    };
}
