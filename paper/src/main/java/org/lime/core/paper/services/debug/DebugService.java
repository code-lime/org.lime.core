package org.lime.core.paper.services.debug;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.TriState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lime.core.common.api.BindService;
import org.lime.core.common.api.RequireCommand;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.Disposable;
import org.lime.core.paper.commands.NativeCommandConsumerFactory;
import org.lime.core.paper.services.buffers.EntityBufferStorage;
import org.lime.core.paper.services.buffers.IterationEntityBuffer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@BindService
public class DebugService
        implements Service {
    private static final Duration DELAY = Duration.ofSeconds(1);
    private static final String SHOW_TAG = "debug.show";
    private static final Vector3fc SHAPE_OFFSET = new Vector3f(0.01f);
    private static final int VIEW_RANGE = 10000;

    @Inject ScheduleTaskService taskService;
    @Inject NativeCommandConsumerFactory consumerFactory;
    @Inject EntityBufferStorage bufferStorage;
    @Inject World world;
    @Inject Plugin plugin;

    private boolean enable = false;
    private IterationEntityBuffer<TextDisplay> textBuffer;
    private IterationEntityBuffer<BlockDisplay> blockBuffer;
    private final List<DebugReader> debugs = new ArrayList<>();

    @RequireCommand
    CommandConsumer<?> command() {
        return consumerFactory.of("record", j -> j
                .then(consumerFactory.literal("debug")
                        .requires(consumerFactory.operator())
                        .then(consumerFactory.literal("enable")
                                .executes(ctx -> {
                                    debug(ctx.getSource().getSender(), TriState.TRUE);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(consumerFactory.literal("disable")
                                .executes(ctx -> {
                                    debug(ctx.getSource().getSender(), TriState.TRUE);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(consumerFactory.literal("show")
                                .requires(ctx -> ctx.getSender() instanceof Player)
                                .executes(ctx -> {
                                    Player player = ((Player)ctx.getSource().getSender());
                                    show(player, true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(consumerFactory.literal("hide")
                                .requires(ctx -> ctx.getSender() instanceof Player)
                                .executes(ctx -> {
                                    Player player = ((Player)ctx.getSource().getSender());
                                    show(player, false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .executes(ctx -> {
                            debug(ctx.getSource().getSender(), TriState.NOT_SET);
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    @Override
    public Disposable register() {
        Disposable.Composite composite = Disposable.composite();

        composite.add(blockBuffer = bufferStorage.block("core-debug-block", world));
        composite.add(textBuffer = bufferStorage.text("core-debug-text", world));
        composite.add(taskService.builder()
                .withCallback(this::update)
                .withWait(DELAY)
                .withLoop(DELAY)
                .execute()::cancel);

        return composite;
    }

    public Disposable registerDebug(DebugReader debugReader) {
        debugs.add(debugReader);
        return () -> debugs.remove(debugReader);
    }

    private void debug(Audience audience, TriState enable) {
        this.enable = switch (enable) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> !this.enable;
        };
        audience.sendMessage(Component.empty()
                .append(Component.text("Debug: "))
                .append(this.enable
                        ? Component.text("ENABLED").color(NamedTextColor.GREEN)
                        : Component.text("DISABLED").color(NamedTextColor.RED)));
        blockBuffer.use().close();
        textBuffer.use().close();
    }
    private void show(Player player, boolean show) {
        Set<String> tags = player.getScoreboardTags();
        if (show) tags.add(SHOW_TAG);
        else tags.remove(SHOW_TAG);
        player.sendMessage(Component.empty()
                .append(Component.text("Debug self: "))
                .append(show
                        ? Component.text("SHOW").color(NamedTextColor.GREEN)
                        : Component.text("HIDE").color(NamedTextColor.RED)));
        blockBuffer.use().close();
        textBuffer.use().close();
    }

    private void update() {
        if (!enable)
            return;
        try (
                var ignored = blockBuffer.use();
                var ignored1 = textBuffer.use()) {

            debugs.forEach(debug -> {
                debug.readShapes(this::renderShape);
                debug.readPoints(this::renderPoint);
            });
        }
    }
    private void renderShape(World world, AABB aabb, TextColor color, @Nullable NameInfo name) {
        var center = aabb.getCenter();
        if (name != null && !name.name().isBlank()) {
            var textDisplay = textBuffer.nextBuffer(name.renderLocation(world, aabb));
            textDisplay.text(Component.text(name.name())
                    .color(color));
            textDisplay.setBackgroundColor(Color.fromRGB(NamedTextColor.BLACK.value()));
            textDisplay.setSeeThrough(true);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
            float globalSize = name.scale() * (float) aabb.getSize();
            textDisplay.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(globalSize, globalSize, globalSize).add(SHAPE_OFFSET),
                    new Quaternionf()));
            configureDefault(textDisplay, color);
        }

        for (int i = 0; i <= 1; i++) {
            float modify = i == 0 ? 1 : -1;
            var blockDisplay = blockBuffer.nextBuffer(new Location(world, center.x, center.y, center.z, 0, 0));
            blockDisplay.setBlock(Material.GLASS.createBlockData());

            blockDisplay.setTransformationMatrix(new Matrix4f()
                    .scale(new Vector3f((float) aabb.getXsize(), (float) aabb.getYsize(), (float) aabb.getZsize()).add(SHAPE_OFFSET).mul(modify))
                    .mul(new Matrix4f()
                            .translation(new Vector3f(-0.5f, -0.5f, -0.5f))));
            configureDefault(blockDisplay, color);
        }
    }
    private void configureDefault(Display display, TextColor color) {
        display.setVisibleByDefault(false);
        display.setViewRange(VIEW_RANGE);
        display.setDisplayWidth(VIEW_RANGE);
        display.setDisplayHeight(VIEW_RANGE);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setGlowing(true);
        display.setGlowColorOverride(Color.fromRGB(color.value()));

        Bukkit.getOnlinePlayers().forEach(player -> {
            boolean actualSee = player.canSee(display);
            boolean requireSee = player.getScoreboardTags().contains(SHOW_TAG);
            if (actualSee == requireSee)
                return;
            if (requireSee) player.showEntity(plugin, display);
            else player.hideEntity(plugin, display);
        });
    }
    private void renderPoint(World world, Vec3 pos, TextColor color, @Nullable NameInfo name) {
        renderShape(world, AABB.ofSize(pos, 1, 1, 1), color, name);
    }
}
