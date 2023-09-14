package org.lime.system.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lime.system.json;
import org.lime.system.toast.*;

public class MathUtils {
    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static String getDouble(double value) {
        return getDouble(value, 3);
    }
    public static String getDouble(double value, int parts) {
        return String.format("%1."+parts+"f", value).replace(",", ".");
    }

    public static Toast3<Integer, Integer, Integer> getPosToast(String str) {
        String[] _pos = str.split(" ");
        return Toast.of(Integer.parseInt(_pos[0]), Integer.parseInt(_pos[1]), Integer.parseInt(_pos[2]));
    }
    public static Vector getVector(String str) {
        String[] _pos = str.split(" ");
        return new Vector(Double.parseDouble(_pos[0]), Double.parseDouble(_pos[1]), Double.parseDouble(_pos[2]));
    }
    public static Location getLocation(World world, String str) {
        return getLocation(world, str, true);
    }
    public static Location getLocation(World world, String str, boolean isCorner) {
        String[] _pos = str.split(" ");
        double centerOffset = isCorner ? 0.5 : 0;
        Location location = new Location(world, Double.parseDouble(_pos[0]) + centerOffset, Double.parseDouble(_pos[1]), Double.parseDouble(_pos[2]) + centerOffset);
        if (_pos.length >= 5) {
            location.setYaw(Float.parseFloat(_pos[3]));
            location.setPitch(Float.parseFloat(_pos[4]));
        }
        return location;
    }
    public static String getString(Vector str) {
        return String.format("%1.3f %1.3f %1.3f", str.getX(), str.getY(), str.getZ()).replace(",", ".");
    }
    public static String getString(Location str) {
        return String.format("%1.3f %1.3f %1.3f %1.3f %1.3f", str.getX(), str.getY(), str.getZ(), (double)str.getYaw(), (double)str.getPitch()).replace(",", ".");
    }
    private static StringBuilder padInt(int value, char ch, int length) {
        StringBuilder builder = new StringBuilder();
        builder.append(value);
        length -= builder.length();
        for (int i = 0; i < length; i++)
            builder.insert(0, ch);
        return builder;
    }
    public static String getIntString(Vector str, String separator) {
        return padInt(str.getBlockX(), ' ', 5) + separator + padInt(str.getBlockY(), ' ', 3) + separator + padInt(str.getBlockZ(), ' ', 5);
    }

    public static Vector3f convert(Vector pos) {
        return new Vector3f((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
    }
    public static Vector convert(Vector3f pos) {
        return new Vector(pos.x, pos.y, pos.z);
    }
    public static Quaternionf quaternion(JsonElement json) {
        if (json.isJsonPrimitive()) {
            Vector euler = getVector(json.getAsString());
            return new Quaternionf().rotationXYZ((float) Math.toRadians(euler.getX()), (float) Math.toRadians(euler.getY()), (float) Math.toRadians(euler.getZ()));
        } else if (json.isJsonObject()) {
            Quaternionf quaternion = new Quaternionf();
            json.getAsJsonObject()
                    .entrySet()
                    .forEach(kv -> quaternion.rotateAxis((float) Math.toRadians(kv.getValue().getAsFloat()), convert(getVector(kv.getKey()))));
            return quaternion;
        } else if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            if (array.size() == 4 && array.get(0).isJsonPrimitive())
                return new Quaternionf(
                        array.get(0).getAsFloat(),
                        array.get(1).getAsFloat(),
                        array.get(2).getAsFloat(),
                        array.get(3).getAsFloat()
                );
            Quaternionf quaternion = new Quaternionf();
            array.forEach(kv -> {
                JsonObject step = kv.getAsJsonObject();
                quaternion.rotateAxis((float) Math.toRadians(step.get("angle").getAsFloat()), convert(getVector(step.get("axis").getAsString())));
            });
            return quaternion;
        } else return new Quaternionf();
    }
    public static JsonElement quaternion(Quaternionf quaternion) {
        return json.array()
                .add(quaternion.x)
                .add(quaternion.y)
                .add(quaternion.z)
                .add(quaternion.w)
                .build();
    }
    public static Transformation transformation(Location location) {
        Quaternionf rotation = new Quaternionf()
                .rotationAxis((float)Math.toRadians(location.getPitch()), new Vector3f(1, 0, 0))
                .rotationAxis((float)Math.toRadians(location.getYaw()), new Vector3f(0, 1, 0));
        return new Transformation(convert(location.toVector()), rotation, null, null);
    }
    public static Transformation transformation(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject data = json.getAsJsonObject();

            Vector3f offset = null;
            Vector3f scale = null;

            Quaternionf leftRotation = null;
            Quaternionf rightRotation = null;

            if (data.has("offset")) offset = convert(getVector(data.get("offset").getAsString()));
            if (data.has("scale")) scale = convert(getVector(data.get("scale").getAsString()));
            if (data.has("rotation")) {
                JsonElement rotation = data.get("rotation");
                if (rotation.isJsonPrimitive()) leftRotation = quaternion(rotation);
                else {
                    JsonObject _rot = rotation.getAsJsonObject();
                    if (_rot.has("left")) leftRotation = quaternion(_rot.get("left"));
                    if (_rot.has("right")) rightRotation = quaternion(_rot.get("right"));
                }
            }
            return new Transformation(offset, leftRotation, scale, rightRotation);
        }

        return transformation(getLocation(null, json.getAsString(), false));
    }
    public static JsonObject transformation(Transformation transformation) {
        return json.object()
                .add("offset", getString(convert(transformation.getTranslation())))
                .add("scale", getString(convert(transformation.getScale())))
                .addObject("rotation", v -> v
                        .add("left", quaternion(transformation.getLeftRotation()))
                        .add("right", quaternion(transformation.getRightRotation()))
                )
                .build();
    }
}
