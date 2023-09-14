package org.lime;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.lime.system.toast.*;

import java.util.Objects;

public class Position implements Cloneable {
    public final World world;
    public final int x;
    public final int y;
    public final int z;

    public Position(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Position(World world, Vector pos) {
        this(world, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
    }
    public Position(Location location) {
        this(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    public Position(Block block) {
        this(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public static Position of(World world, int x, int y, int z) {
        return new Position(world, x, y, z);
    }
    public static Position of(World world, Vector pos) {
        return new Position(world, pos);
    }
    public static Position of(Location location) {
        return new Position(location);
    }
    public static Position of(Block block) {
        return new Position(block);
    }

    public Position add(int x, int y, int z) {
        return new Position(world, this.x + x, this.y + y, this.z + z);
    }
    public Position del(int x, int y, int z) {
        return new Position(world, this.x - x, this.y - y, this.z - z);
    }
    public Position offset(Toast3<Integer, Integer, Integer> offset) {
        return add(offset.val0, offset.val1, offset.val2);
    }

    public Vector toVector() { return new Vector(x, y, z); }
    public Location getLocation() {
        return new Location(world, x, y, z);
    }
    public Location getLocation(int x, int y, int z) {
        return new Location(world, this.x + x, this.y + y, this.z + z);
    }
    public Location getLocation(Toast3<Integer, Integer, Integer> offset) {
        return getLocation(offset.val0, offset.val1, offset.val2);
    }
    public Location getLocation(double x, double y, double z) {
        return new Location(world, this.x + x, this.y + y, this.z + z);
    }
    public Location getLocation(Vector offset) {
        return getLocation(offset.getX(), offset.getY(), offset.getZ());
    }
    public Block getBlock() {
        return world.getBlockAt(x,y,z);
    }

    @Override public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
    @Override public boolean equals(Object obj) {
        if (obj instanceof Position) return equals((Position)obj);
        if (obj instanceof Location) return equals(new Position((Location)obj));
        return false;
    }

    public boolean equals(Position obj) {
        return obj != null && obj.x == x && obj.y == y && obj.z == z && obj.world == world;
    }

    @Override public String toString() { return toString(true); }
    public String toString(boolean showWorld) { return x + "," + y + "," + z + (showWorld ? (":" + world.getUID().toString()) : ""); }
    public String toSave() { return x + " " + y + " " + z; }
}









