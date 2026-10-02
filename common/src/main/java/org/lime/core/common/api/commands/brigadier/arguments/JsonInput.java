package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.gson.*;
import com.google.gson.annotations.*;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.*;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.Function;

public final class JsonInput {
    private static final View ANY_VIEW = new View(true, false, Set.of(), Map.of(), null, Set.of(), List.of(), null, 0, Integer.MAX_VALUE, List.of());
    private static final View NONE_VIEW = new View(false, true, Set.of(), Map.of(), null, Set.of(), List.of(), null, 0, 0, List.of());
    private static final Node ANY = new Node(() -> ANY_VIEW);
    private static final Node NONE = new Node(() -> NONE_VIEW);

    private final Node root;

    private JsonInput(Node root) {
        this.root = root;
    }

    public static @NotNull JsonInput raw() {
        return new JsonInput(ANY);
    }

    public static @NotNull JsonInput of(@NotNull Gson gson, @NotNull TypeToken<?> type) {
        return new JsonInput(new Builder(gson).input(type));
    }

    public static @NotNull JsonInput of(@NotNull Gson gson, @NotNull Provider provider) {
        return new JsonInput(provider.input(new Builder(gson)));
    }

    public static @NotNull JsonInput of(@NotNull Node root) {
        return new JsonInput(root);
    }

    public static @NotNull Node any() {
        return ANY;
    }

    public static @NotNull Node none() {
        return NONE;
    }

    public static @NotNull Node scalar(@NotNull Type type) {
        return scalar(type, List.of());
    }

    public static @NotNull Node scalar(@NotNull Type type, @NotNull List<JsonElement> values) {
        View view = new View(false, false, Set.of(type), Map.of(), null, Set.of(), List.of(), null, 0, Integer.MAX_VALUE, values);
        return new Node(() -> view);
    }

    public static @NotNull Node object(@NotNull Map<String, Node> properties) {
        return object(properties, NONE, Set.of());
    }

    public static @NotNull Node object(@NotNull Map<String, Node> properties, @NotNull Node additional, @NotNull Set<String> required) {
        View view = new View(false, false, Set.of(Type.OBJECT), properties, additional, required, List.of(), null, 0, Integer.MAX_VALUE, List.of());
        return new Node(() -> view);
    }

    public static @NotNull Node array(@NotNull Node items) {
        View view = new View(false, false, Set.of(Type.ARRAY), Map.of(), null, Set.of(), List.of(), items, 0, Integer.MAX_VALUE, List.of());
        return new Node(() -> view);
    }

    public static @NotNull Node tuple(@NotNull List<Node> items) {
        View view = new View(false, false, Set.of(Type.ARRAY), Map.of(), null, Set.of(), items, NONE, items.size(), items.size(), List.of());
        return new Node(() -> view);
    }

    public static @NotNull Node choice(@NotNull List<Node> values) {
        return new Node(() -> merge(values, true));
    }

    public static @NotNull Node all(@NotNull List<Node> values) {
        return new Node(() -> merge(values, false));
    }

    public static @NotNull Node dependent(@NotNull String sibling, @NotNull Function<JsonElement, Node> resolver) {
        return new Node(() -> NONE_VIEW, sibling, resolver);
    }

    public View root() {
        return root.view();
    }

    public View at(List<Object> path) {
        Node node = root;
        for (Object part : path)
            node = node.view().child(part);
        return node.view();
    }

    public View at(List<Object> path, JsonElement context) {
        Node node = root;
        JsonElement current = context;
        JsonElement parent = context;
        for (Object part : path) {
            if (node.sibling != null) {
                if (!parent.isJsonObject() || !parent.getAsJsonObject().has(node.sibling))
                    return NONE_VIEW;
                node = node.resolver.apply(parent.getAsJsonObject().get(node.sibling));
            }
            node = node.view().child(part);
            parent = current;
            if (current.isJsonObject() && part instanceof String key && current.getAsJsonObject().has(key))
                current = current.getAsJsonObject().get(key);
            else if (current.isJsonArray() && part instanceof Integer index && index < current.getAsJsonArray().size())
                current = current.getAsJsonArray().get(index);
            else
                current = JsonNull.INSTANCE;
        }
        if (node.sibling != null) {
            if (!parent.isJsonObject() || !parent.getAsJsonObject().has(node.sibling))
                return NONE_VIEW;
            node = node.resolver.apply(parent.getAsJsonObject().get(node.sibling));
        }
        return node.view();
    }

    public JsonElement normalizeAndValidate(View view, List<Object> path, JsonElement value) {
        JsonElement normalized = normalize(view, value);
        String location = "$";
        for (Object part : path)
            location += part instanceof String name ? "." + name : "[" + part + "]";
        validate(view, normalized, location);
        return normalized;
    }

    public JsonElement normalize(JsonElement value) {
        return normalize(root, value, null);
    }

    public void validate(@NotNull JsonElement value) {
        normalizeAndValidate(value);
    }

    public JsonElement normalizeAndValidate(@NotNull JsonElement value) {
        JsonElement normalized = normalize(value);
        validate(root, normalized, null, "$");
        return normalized;
    }

    private void validate(Node node, JsonElement value, @Nullable JsonObject parent, String path) {
        if (node.sibling != null) {
            if (parent == null || !parent.has(node.sibling))
                throw new ValidationException("Missing sibling '" + node.sibling + "' at " + path);
            node = node.resolver.apply(parent.get(node.sibling));
        }
        validate(node.view(), value, path);
    }

    private void validate(View view, JsonElement value, String path) {
        if (view.any())
            return;
        if (value == null || value.isJsonNull()) {
            if (!view.any() && !view.types().contains(Type.NULL))
                throw new ValidationException("Null is not allowed at " + path);
            return;
        }
        Type actual = value.isJsonObject() ? Type.OBJECT : value.isJsonArray() ? Type.ARRAY : value.getAsJsonPrimitive().isBoolean() ? Type.BOOLEAN
                : value.getAsJsonPrimitive().isString() ? Type.STRING : value.getAsJsonPrimitive().getAsBigDecimal().stripTrailingZeros().scale() <= 0 ? Type.INTEGER : Type.NUMBER;
        if (!view.any() && (view.none() || !view.types().contains(actual) && !(actual == Type.INTEGER && view.types().contains(Type.NUMBER))))
            throw new ValidationException("Invalid value type at " + path + ": " + actual);
        if (!view.values().isEmpty() && view.values().stream().noneMatch(value::equals))
            throw new ValidationException("Invalid value at " + path + ": " + value);
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (String required : view.required())
                if (!object.has(required))
                    throw new ValidationException("Missing required property '" + required + "' at " + path);
            for (var entry : object.entrySet()) {
                Node child = view.child(entry.getKey());
                validate(child, entry.getValue(), object, path + "." + entry.getKey());
            }
        } else if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            if (array.size() < view.minItems() || array.size() > view.maxItems())
                throw new ValidationException("Invalid array length at " + path);
            for (int i = 0; i < array.size(); i++)
                validate(view.child(i), array.get(i), null, path + "[" + i + "]");
        }
    }

    private JsonElement normalize(Node node, JsonElement value, @Nullable JsonObject parent) {
        if (node.sibling != null) {
            if (parent == null || !parent.has(node.sibling))
                return value;
            node = node.resolver.apply(parent.get(node.sibling));
        }
        return normalize(node.view(), value);
    }

    private JsonElement normalize(View view, JsonElement value) {
        if (view.any())
            return value;
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isNumber() && view.types().contains(Type.BOOLEAN) && view.types().stream().allMatch(type -> type == Type.BOOLEAN || type == Type.NULL)) {
                BigDecimal number = primitive.getAsBigDecimal();
                if (number.compareTo(BigDecimal.ZERO) == 0)
                    return new JsonPrimitive(false);
                if (number.compareTo(BigDecimal.ONE) == 0)
                    return new JsonPrimitive(true);
            }
        } else if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            for (int i = 0; i < array.size(); i++)
                array.set(i, normalize(view.child(i), array.get(i), null));
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            object.entrySet().forEach(entry -> entry.setValue(normalize(view.child(entry.getKey()), entry.getValue(), object)));
        }
        return value;
    }

    public static final class ValidationException extends IllegalArgumentException {
        private ValidationException(String message) {
            super(message);
        }
    }

    public interface Provider {
        @NotNull Node input(@NotNull Context context);
    }

    public interface Context {
        @NotNull Node input(@NotNull TypeToken<?> type);
    }

    public enum Type {
        OBJECT(List.of("{")),
        ARRAY(List.of("[")),
        STRING(List.of("\"\"")),
        INTEGER(List.of("0")),
        NULL(List.of()),
        NUMBER(List.of("0")),
        BOOLEAN(List.of("true", "false")),
        ;

        private final List<String> suggestions;

        Type(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        List<String> suggestions() {
            return suggestions;
        }
    }

    public static final class Node {
        private final Supplier<View> view;
        private final @Nullable String sibling;
        private final @Nullable Function<JsonElement, Node> resolver;

        private Node(Supplier<View> view) {
            this(view, null, null);
        }

        private Node(Supplier<View> view, @Nullable String sibling, @Nullable Function<JsonElement, Node> resolver) {
            this.view = view;
            this.sibling = sibling;
            this.resolver = resolver;
        }

        private View view() {
            return view.get();
        }
    }

    public record View(
            boolean any,
            boolean none,
            Set<Type> types, Map<String, Node> properties,
            @Nullable Node additional, Set<String> required,
            List<Node> prefixItems, @Nullable Node items, int minItems, int maxItems,
            List<JsonElement> values) {
        public Node child(Object part) {
            if (any)
                return ANY;
            if (none)
                return NONE;
            if (part instanceof String property)
                return properties.getOrDefault(property, additional == null ? NONE : additional);
            int index = (Integer)part;
            if (index < prefixItems.size())
                return prefixItems.get(index);
            return items == null ? NONE : items;
        }

        public boolean additionalPropertiesAllowed() {
            return additional != null && !additional.view().none;
        }

        public boolean canCloseObject(Set<String> used) {
            return used.containsAll(required);
        }

        public boolean canAddArrayItem(int index) {
            return index < maxItems && !child(index).view().none;
        }

        public boolean canCloseArray(int count) {
            return count >= minItems;
        }
    }

    private static View merge(List<Node> nodes, boolean choice) {
        List<View> values = nodes.stream().map(Node::view).filter(view -> choice ? !view.none : !view.any).toList();
        if (values.isEmpty())
            return choice ? NONE_VIEW : ANY_VIEW;
        if ((choice && values.stream().anyMatch(View::any)) || (!choice && values.stream().anyMatch(View::none)))
            return choice ? ANY_VIEW : NONE_VIEW;

        Set<Type> types = new LinkedHashSet<>(values.get(0).types);
        if (choice)
            values.stream().skip(1).forEach(view -> types.addAll(view.types));
        else
            values.stream().skip(1).forEach(view -> types.retainAll(view.types));
        if (types.isEmpty())
            return NONE_VIEW;

        Set<String> keys = new LinkedHashSet<>();
        values.forEach(view -> keys.addAll(view.properties.keySet()));
        Map<String, Node> properties = new LinkedHashMap<>();
        keys.forEach(key -> properties.put(key, combineChildren(values, key, choice)));

        List<View> objectValues = values.stream().filter(view -> view.types.contains(Type.OBJECT)).toList();
        List<Node> additional = objectValues.stream()
                .map(view -> view.additional == null ? NONE : view.additional).toList();
        Set<String> required = objectValues.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(objectValues.get(0).required);
        if (choice)
            objectValues.stream().skip(1).forEach(view -> required.retainAll(view.required));
        else
            objectValues.stream().skip(1).forEach(view -> required.addAll(view.required));

        List<View> arrayValues = values.stream().filter(view -> view.types.contains(Type.ARRAY)).toList();
        int prefixSize = arrayValues.stream().mapToInt(view -> view.prefixItems.size()).max().orElse(0);
        List<Node> prefix = new ArrayList<>();
        for (int i = 0; i < prefixSize; i++)
            prefix.add(combineChildren(arrayValues, i, choice));
        Node items = combineChildren(arrayValues, prefixSize, choice);
        int minItems = choice ? arrayValues.stream().mapToInt(View::minItems).min().orElse(0) : arrayValues.stream().mapToInt(View::minItems).max().orElse(0);
        int maxItems = choice ? arrayValues.stream().mapToInt(View::maxItems).max().orElse(Integer.MAX_VALUE) : arrayValues.stream().mapToInt(View::maxItems).min().orElse(Integer.MAX_VALUE);

        List<JsonElement> suggestions = new ArrayList<>();
        if (choice) {
            values.forEach(view -> view.values.forEach(value -> {
                if (!suggestions.contains(value))
                    suggestions.add(value);
            }));
        } else {
            values.stream().map(View::values).filter(list -> !list.isEmpty()).findFirst().ifPresent(suggestions::addAll);
            values.stream().map(View::values).filter(list -> !list.isEmpty()).skip(1).forEach(suggestions::retainAll);
        }

        return new View(false, false, types, properties, combine(additional, choice), required, prefix, items, minItems, maxItems, suggestions);
    }

    private static Node combineChildren(List<View> views, Object part, boolean choice) {
        List<Node> nodes = new ArrayList<>();
        for (View view : views) {
            if (part instanceof String && !view.types.contains(Type.OBJECT) || part instanceof Integer && !view.types.contains(Type.ARRAY)) {
                if (!choice)
                    nodes.add(ANY);
                continue;
            }
            if (part instanceof String property && !view.properties.containsKey(property) && !choice) {
                nodes.add(ANY);
                continue;
            }
            nodes.add(view.child(part));
        }
        return combine(nodes, choice);
    }

    private static Node combine(List<Node> nodes, boolean choice) {
        if (nodes.isEmpty())
            return choice ? NONE : ANY;
        if (nodes.size() == 1)
            return nodes.get(0);
        return choice ? choice(nodes) : all(nodes);
    }

    private static final class Builder implements Context {
        private final Gson gson;
        private final Map<java.lang.reflect.Type, Node> inputs = new LinkedHashMap<>();

        private Builder(Gson gson) {
            this.gson = gson;
        }

        @Override
        public @NotNull Node input(@NotNull TypeToken<?> type) {
            java.lang.reflect.Type key = type.getType();
            Node existing = inputs.get(key);
            if (existing != null)
                return existing;
            Node reference = new Node(() -> inputs.get(key).view());
            inputs.put(key, reference);
            Node input = infer(type);
            inputs.put(key, input);
            return input;
        }

        private Node infer(TypeToken<?> type) {
            TypeAdapter<?> adapter = gson.getAdapter(type);
            if (adapter instanceof Provider provider)
                return provider.input(this);

            Class<?> raw = type.getRawType();
            if (raw.isEnum()) {
                List<JsonElement> values = Arrays.stream(raw.getEnumConstants()).map(value -> gson.toJsonTree(value, type.getType())).toList();
                return scalar(Type.STRING, values);
            }
            if (!standardAdapter(adapter) || raw.isAnnotationPresent(JsonAdapter.class))
                throw new IllegalStateException("JSON input is required for custom adapter of " + type);
            if (JsonElement.class.isAssignableFrom(raw) || raw == Object.class)
                return ANY;
            if (raw == String.class || raw == Character.class || raw == char.class)
                return scalar(Type.STRING);
            if (raw == Boolean.class || raw == boolean.class)
                return scalar(Type.BOOLEAN);
            if (raw == Byte.class || raw == byte.class || raw == Short.class || raw == short.class || raw == Integer.class || raw == int.class || raw == Long.class || raw == long.class)
                return scalar(Type.INTEGER);
            if (Number.class.isAssignableFrom(raw) || raw == float.class || raw == double.class)
                return scalar(Type.NUMBER);
            if (raw.isArray() || type.getType() instanceof GenericArrayType)
                return array(input(component(type)));
            if (Collection.class.isAssignableFrom(raw))
                return array(input(argument(type, Collection.class, 0)));
            if (Map.class.isAssignableFrom(raw))
                return map(type);
            return object(type, raw);
        }

        private Node map(TypeToken<?> type) {
            TypeToken<?> key = argument(type, Map.class, 0);
            Node value = input(argument(type, Map.class, 1));
            Class<?> raw = key.getRawType();
            if (raw.isEnum()) {
                Map<String, Node> properties = new LinkedHashMap<>();
                Arrays.stream(raw.getEnumConstants()).map(item -> gson.toJsonTree(item, key.getType()).getAsString()).forEach(name -> properties.put(name, value));
                return JsonInput.object(properties);
            }
            if (raw == String.class || raw == Character.class || raw == char.class || raw.isPrimitive() || Number.class.isAssignableFrom(raw) || raw == Boolean.class)
                return JsonInput.object(Map.of(), value, Set.of());
            return array(tuple(List.of(input(key), value)));
        }

        private Node object(TypeToken<?> type, Class<?> raw) {
            Map<String, Node> properties = new LinkedHashMap<>();
            for (Class<?> current = raw; current != Object.class && current != null; current = current.getSuperclass()) {
                Map<TypeVariable<?>, java.lang.reflect.Type> arguments = TypeUtils.getTypeArguments(type.getType(), current);
                if (arguments == null)
                    arguments = Map.of();
                for (Field field : current.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic())
                        continue;
                    if (field.isAnnotationPresent(JsonAdapter.class))
                        throw new IllegalStateException("JSON input is required for @JsonAdapter field " + current.getName() + "." + field.getName());
                    SerializedName serialized = field.getAnnotation(SerializedName.class);
                    String name = serialized == null ? FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field) : serialized.value();
                    properties.put(name, input(TypeToken.get(TypeUtils.unrollVariables(arguments, field.getGenericType()))));
                }
            }
            return JsonInput.object(properties);
        }

        private TypeToken<?> component(TypeToken<?> type) {
            java.lang.reflect.Type value = type.getType();
            return TypeToken.get(value instanceof GenericArrayType array ? array.getGenericComponentType() : type.getRawType().getComponentType());
        }

        private TypeToken<?> argument(TypeToken<?> type, Class<?> target, int index) {
            Map<TypeVariable<?>, java.lang.reflect.Type> values = TypeUtils.getTypeArguments(type.getType(), target);
            TypeVariable<?> variable = target.getTypeParameters()[index];
            return TypeToken.get(TypeUtils.unrollVariables(values == null ? Map.of() : values, values == null ? Object.class : values.getOrDefault(variable, Object.class)));
        }

        private boolean standardAdapter(TypeAdapter<?> adapter) {
            String name = adapter.getClass().getName();
            return name.startsWith("com.google.gson.internal.bind.") && !name.contains("TreeTypeAdapter");
        }
    }
}
