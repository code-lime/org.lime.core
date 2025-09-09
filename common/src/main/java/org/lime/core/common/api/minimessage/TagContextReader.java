package org.lime.core.common.api.minimessage;

import com.google.common.base.CaseFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.range.number.BaseNumberRange;
import org.lime.core.common.utils.range.number.IntegerRange;
import org.lime.core.common.utils.system.execute.Func0;
import org.lime.core.common.utils.system.execute.Func1;
import org.lime.core.common.utils.system.execute.FuncEx1;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public record TagContextReader(
        String tag,
        ArgumentQueue args,
        Context context) {
    public <T>T popParse(
            String name,
            int index,
            String parseName,
            FuncEx1<String, T> parse) {
        String raw = args.popOr("The <%s> tag requires exactly #%d argument, the '%s'"
                .formatted(tag(), index + 1, name)).value();
        T value;
        try {
            value = parse.invoke(raw);
        } catch (Throwable e) {
            throw context.newException("Unable to parse a %s '%s' for '%s'. %s"
                    .formatted(parseName, name, raw, e.getMessage()));
        }
        return value;
    }

    public <T extends Number & Comparable<T>>T popNumber(
            String name,
            int index,
            FuncEx1<String, T> parse,
            Func1<T, String> write,
            @Nullable BaseNumberRange<?, T> range) {
        T value = popParse(name, index, "number", parse);
        if (range != null && !range.contains(value))
            throw context.newException("The '%s' is out of range. Range is [%s;%s]"
                    .formatted(name, write.invoke(range.min()), write.invoke(range.max())));
        return value;
    }

    public Key popKey(String name, int index) {
        return popParse(name, index, "key", Key::key);
    }
    public Component popComponent(String name, int index) {
        return popParse(name, index, "component", context::deserialize);
    }
    public <T extends Enum<T>>T popEnum(String name, int index, Class<T> enumClass) {
        return popEnum(name, index, enumClass, List.of(enumClass.getEnumConstants()));
    }
    public <T extends Enum<T>>T popEnum(String name, int index, Class<T> enumClass, List<T> allows) {
        return popParse(name, index, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, enumClass.getSimpleName()), v -> {
            for (T value : allows) {
                if (value.name().equalsIgnoreCase(v))
                    return value;
            }
            throw context.newException("The '%s' is not valid. Allowed: %s"
                    .formatted(name, allows.stream().map(Enum::name).map(String::toLowerCase).collect(Collectors.joining(", "))));
        });
    }
    public int popInt(String name, int index, @Nullable IntegerRange range) {
        return popNumber(name, index, Integer::parseInt, Object::toString, range);
    }
    public int popRadixInt(String name, int index, int radix, @Nullable IntegerRange range) {
        return popNumber(name, index, v -> Integer.parseInt(v, radix), v -> Integer.toString(v, radix), range);
    }
    public int popHexInt(String name, int index, @Nullable IntegerRange range) {
        return popRadixInt(name, index, 16, range);
    }
    public char popHexChar(String name, int index) {
        return (char)popHexInt(name, index, IntegerRange.of(Character.MIN_VALUE, Character.MAX_VALUE));
    }
    public <T>T popSwitch(String name, int index, Map<String, Func0<T>> values) {
        String oneOfKeys = values.keySet().stream().map(v -> "'" + v + "'").collect(Collectors.joining(", "));
        String raw = args.popOr("The <%s> tag requires exactly #%d argument, the '%s' one of %s"
                .formatted(tag(), index + 1, name, oneOfKeys)).value();
        Func0<T> func = values.get(raw);
        if (func == null)
            throw context.newException("Unable to find '%s' in one of %s at '%s'."
                    .formatted(raw, oneOfKeys, name));
        return func.invoke();
    }

    public static TagContextReader of(String tag, ArgumentQueue args, Context context) {
        return new TagContextReader(tag, args, context);
    }
    public static BiFunction<ArgumentQueue, Context, Tag> reader(String tag, Func1<TagContextReader, Tag> reader) {
        return (args, ctx) -> reader.invoke(of(tag, args, ctx));
    }
    public static TagResolver tag(@TagPattern String tag, Func1<TagContextReader, Tag> reader) {
        return TagResolver.resolver(tag, reader(tag, reader));
    }
}
