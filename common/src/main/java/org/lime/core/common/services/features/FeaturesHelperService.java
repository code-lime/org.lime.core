package org.lime.core.common.services.features;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lime.core.common.api.BindService;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.services.CustomArgumentUtility;

@BindService
public class FeaturesHelperService
        implements Service {
    @Inject NativeCommandConsumer.Factory<?,?> consumerFactory;
    @Inject CustomArgumentUtility arguments;

    private <V extends Enum<V> & FeatureType<V>, Sender, Register extends NativeCommandConsumer.NativeRegister<Sender>>CommandConsumer<?> createFeatureCommand(
            NativeCommandConsumer.Factory<Sender, Register> consumerFactory,
            String key,
            String name,
            FeatureAccessField<V> access) {
        return consumerFactory.of(key, j -> j
                .then(consumerFactory.literal("features")
                        .requires(consumerFactory.operator())
                        .then(consumerFactory.literal(name)
                                .then(consumerFactory.argument("type", arguments.builderEnum(consumerFactory.senderClass(), access.valueClass()).build())
                                        .executes(ctx -> {
                                            var type = ctx.getArgument("type", access.valueClass());
                                            access.set(type);
                                            sendStatusMessage(consumerFactory.audience(ctx.getSource()), true, key, name, type);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .executes(ctx -> {
                    sendStatusMessage(consumerFactory.audience(ctx.getSource()), false, key, name, access.get());
                    return Command.SINGLE_SUCCESS;
                }));
    }
    public <V extends Enum<V> & FeatureType<V>>CommandConsumer<?> createFeatureCommand(
            String key,
            String name,
            FeatureAccessField<V> access) {
        return createFeatureCommand(consumerFactory, key, name, access);
    }

    private <V extends Enum<V> & FeatureType<V>>void sendStatusMessage(Audience audience, boolean changed, String key, String name, V type) {
        audience.sendMessage(Component.empty()
                .append(Component.empty()
                        .append(Component.text("["))
                        .append(Component.text(key).color(NamedTextColor.GOLD))
                        .append(Component.text("] ")))
                .append(Component.text(changed ? "Changed feature '" : "Current feature '"))
                .append(Component.text(name).color(NamedTextColor.AQUA))
                .append(Component.text("' status: "))
                .append(Component.text(type.name())
                        .color(type.enable() ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }
}
