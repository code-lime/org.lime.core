package org.lime.core.paper.services.debug;

import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Action4;
import org.lime.core.common.utils.execute.Func0;

import java.util.Collection;

public interface DebugReader {
    default void readPoints(Action4<World, Vec3, TextColor, @Nullable NameInfo> callback) {}
    default void readShapes(Action4<World, AABB, TextColor, @Nullable NameInfo> callback) {}

    static DebugReader combine(Collection<? extends DebugReader> readers) {
        return combine(() -> readers);
    }
    static DebugReader combine(Func0<Collection<? extends DebugReader>> readers) {
        return new DebugReader() {
            @Override
            public void readPoints(Action4<World, Vec3, TextColor, @Nullable NameInfo> callback) {
                readers.invoke().forEach(reader -> reader.readPoints(callback));
            }
            @Override
            public void readShapes(Action4<World, AABB, TextColor, @Nullable NameInfo> callback) {
                readers.invoke().forEach(reader -> reader.readShapes(callback));
            }
        };
    }
}
