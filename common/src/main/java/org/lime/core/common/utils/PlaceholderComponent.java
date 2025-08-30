package org.lime.core.common.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public record PlaceholderComponent(
        MiniMessage miniMessage,
        String rawComponent)
        implements ComponentLike {
    public PlaceholderComponent(String raw) {
        this(MiniMessage.miniMessage(), raw);
    }

    @Override
    public @NotNull Component asComponent() {
        return miniMessage.deserialize(rawComponent);
    }

    public Component renderMiniMessagePlaceholders(
            Map<String, String> placeholders) {
        Map<String, Component> componentPlaceholders = placeholders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> miniMessage.deserialize(entry.getValue())
                ));

        return renderPlaceholders(componentPlaceholders);
    }
    public Component renderPlaceholders(
            Map<String, ? extends ComponentLike> placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        placeholders.forEach((key, component) -> builder.resolver(Placeholder.component(key, component)));
        return miniMessage.deserialize(rawComponent, builder.build());
    }
}
