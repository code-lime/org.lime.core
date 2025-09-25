package org.lime.core.common.utils;

import com.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ComponentUtils {
    public static final Style NORMAL_STYLE = Style.style()
            .color(NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false)
            .build();
    public static Style normalStyle() {
        return NORMAL_STYLE;
    }

    public static Component normalize(ComponentLike component) {
        return Component.empty()
                .style(NORMAL_STYLE)
                .append(component);
    }
    public static List<Component> normalize(Iterable<? extends ComponentLike> components) {
        return Streams.stream(components).map(ComponentUtils::normalize).toList();
    }

    public static Title title(Iterable<? extends ComponentLike> lines, @Nullable Title.Times times) {
        Component title = Component.empty();
        Component subtitle = Component.empty();
        int i = 0;
        for (ComponentLike line : lines) {
            switch (i) {
                case 0 -> title = line.asComponent();
                case 1 -> subtitle = line.asComponent();
            }
            i++;
        }
        return times == null
                ? Title.title(title, subtitle)
                : Title.title(title, subtitle, times);
    }
}
