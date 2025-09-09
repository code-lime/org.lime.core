package org.lime.core.paper.utils.adapters;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.FinePosition;
import io.papermc.paper.math.Position;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.adapters.StringTypeAdapter;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PositionTypeAdapters {
    public static class BaseTypeAdapter
            extends StringTypeAdapter<Position> {
        @Override
        public String write(Position value) {
            return value.x() + " " + value.y() + " " + value.z();
        }
        @Override
        public Position read(String value) {
            String[] parts = value.split(" ");
            return Position.fine(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        }
    }
    public static class FineTypeAdapter
            extends StringTypeAdapter<FinePosition> {
        @Override
        public String write(FinePosition value) {
            return value.x() + " " + value.y() + " " + value.z();
        }
        @Override
        public FinePosition read(String value) {
            String[] parts = value.split(" ");
            return Position.fine(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        }
    }
    public static class BlockTypeAdapter
            extends StringTypeAdapter<BlockPosition> {
        @Override
        public String write(BlockPosition value) {
            return value.blockX() + " " + value.blockY() + " " + value.blockZ();
        }
        @Override
        public BlockPosition read(String value) {
            String[] parts = value.split(" ");
            return Position.block(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        }
    }

    public static class LocationTypeAdapter
            extends StringTypeAdapter<Location> {
        @Override
        public String write(Location value) {
            List<String> items = new ArrayList<>();
            World world = value.getWorld();
            if (world != null)
                items.add(world.key().toString());

            items.add(String.valueOf(value.getX()));
            items.add(String.valueOf(value.getY()));
            items.add(String.valueOf(value.getZ()));

            float yaw = value.getYaw();
            float pitch = value.getPitch();

            if (yaw != 0 || pitch != 0) {
                items.add(String.valueOf(yaw));
                items.add(String.valueOf(pitch));
            }

            return String.join(" ", items);
        }
        @Override
        public Location read(String value) {
            String[] parts = value.split(" ");

            boolean hasWorld;
            boolean hasRotation;

            switch (parts.length) {
                case 3:
                    hasWorld = false;
                    hasRotation = false;
                    break;
                case 4:
                    hasWorld = true;
                    hasRotation = false;
                    break;
                case 5:
                    hasWorld = false;
                    hasRotation = true;
                    break;
                case 6:
                    hasWorld = true;
                    hasRotation = true;
                    break;
                default:
                    throw new IllegalArgumentException(value + " is not location");
            }

            int index = 0;

            @Nullable World world = null;
            if (hasWorld) {
                world = Bukkit.getWorld(Key.key(parts[index++]));
            }
            double x = Double.parseDouble(parts[index++]);
            double y = Double.parseDouble(parts[index++]);
            double z = Double.parseDouble(parts[index++]);
            float yaw = 0;
            float pitch = 0;
            if (hasRotation) {
                yaw = Float.parseFloat(parts[index++]);
                pitch = Float.parseFloat(parts[index++]);
            }

            return new Location(world, x, y, z, yaw, pitch);
        }
    }
}
