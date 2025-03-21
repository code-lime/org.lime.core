package org.lime.system.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.joml.*;
import org.joml.Math;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.json.builder.Json;
import org.lime.system.tuple.*;

import javax.annotation.Nullable;

public class MathUtils {
    public static double round(double value, int places) {
        double scale = java.lang.Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static String getDouble(double value) {
        return getDouble(value, 3);
    }
    public static String getDouble(double value, int parts) {
        return String.format("%1."+parts+"f", value).replace(",", ".");
    }

    public static Tuple3<Integer, Integer, Integer> getPosTuple(String str) {
        String[] _pos = str.split(" ");
        return Tuple.of(Integer.parseInt(_pos[0]), Integer.parseInt(_pos[1]), Integer.parseInt(_pos[2]));
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

    /*
Vector3f xAxis = new Vector3f(-sinYaw, 0, cosYaw);
Vector3f yAxis = new Vector3f(cosYaw, 0, sinYaw);
Vector3f zAxis = new Vector3f(0, cosYaw, 0);

---
Vector3f xAxis = new Vector3f(cosYaw, -sinYaw, 0);
Vector3f yAxis = new Vector3f(sinYaw, cosYaw, 0);
Vector3f zAxis = new Vector3f(0, 0, cosYaw);
---
(1,2,3) -> (2,1,3)

Vector3f xAxis = new Vector3f(
     cosYaw*sinPitch*sinRoll - sinYaw*cosRoll,
     cosYaw*cosPitch,
     cosYaw*sinPitch*cosRoll + sinYaw*sinRoll,
);
Vector3f zAxis = new Vector3f(
    cosPitch*sinRoll,
    -sinPitch,
    cosPitch*cosYaw,
);
---

    */
    public static Vector3f directionForward(float yaw, float pitch, float roll) {
        float sinYaw = Math.sin(yaw);
        float cosYaw = Math.cos(yaw);
        float sinPitch = Math.sin(pitch);
        float cosPitch = Math.cos(pitch);
        float sinRoll = Math.sin(roll);
        float cosRoll = Math.cos(roll);

        return new Vector3f(
                cosYaw*sinPitch*sinRoll - sinYaw*cosRoll,
                cosYaw*cosPitch,
                cosYaw*sinPitch*cosRoll + sinYaw*sinRoll
        );
    }
    public static Vector3f directionRight(float yaw, float pitch, float roll) {
        float sinYaw = Math.sin(yaw);
        float cosYaw = Math.cos(yaw);
        float sinPitch = Math.sin(pitch);
        float cosPitch = Math.cos(pitch);
        float sinRoll = Math.sin(roll);
        float cosRoll = Math.cos(roll);
        return new Vector3f(
                sinYaw*sinPitch*sinRoll + cosYaw*cosRoll,
                sinYaw*cosPitch,
                sinYaw*sinPitch*cosRoll - cosYaw*sinRoll
        );
    }
    public static Vector3f directionUp(float yaw, float pitch, float roll) {
        float cosYaw = Math.cos(yaw);
        float sinPitch = Math.sin(pitch);
        float cosPitch = Math.cos(pitch);
        float sinRoll = Math.sin(roll);
        return new Vector3f(
                cosPitch*sinRoll,
                -sinPitch,
                cosPitch*cosYaw
        );
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
                    .forEach(kv -> quaternion.rotateAxis(Math.toRadians(kv.getValue().getAsFloat()), convert(getVector(kv.getKey()))));
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
                quaternion.rotateAxis(Math.toRadians(step.get("angle").getAsFloat()), convert(getVector(step.get("axis").getAsString())));
            });
            return quaternion;
        } else return new Quaternionf();
    }
    public static Quaternionf quaternion(float yaw, float pitch) {
        return new Quaternionf().rotateZYX(0, Math.toRadians(-yaw), Math.toRadians(pitch));
    }
    public static JsonElement quaternion(Quaternionf quaternion) {
        return Json.array()
                .add(quaternion.x)
                .add(quaternion.y)
                .add(quaternion.z)
                .add(quaternion.w)
                .build();
    }
    public static Transformation transformation(Location location) {
        Quaternionf rotation = new Quaternionf().rotateZYX(0, Math.toRadians(-location.getYaw()), Math.toRadians(location.getPitch()));
        /*
        Quaternionf rotation = new Quaternionf()
                .rotationAxis((float)Math.toRadians(location.getPitch()), new Vector3f(0, 0, 1))
                .rotationAxis((float)Math.toRadians(-location.getYaw()), new Vector3f(0, 1, 0));
        */
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
        return Json.object()
                .add("offset", getString(convert(transformation.getTranslation())))
                .add("scale", getString(convert(transformation.getScale())))
                .addObject("rotation", v -> v
                        .add("left", quaternion(transformation.getLeftRotation()))
                        .add("right", quaternion(transformation.getRightRotation()))
                )
                .build();
    }
    public static Transformation transform(@Nullable Transformation parent, Transformation child) {
        if (parent == null) return child;
        Matrix4f current = child.getMatrix();
        current.mul(parent.getMatrix());
        return new Transformation(current);
    }
    public static Transformation onlyYaw(Transformation transformation) {
        Matrix4f matrix4f = transformation.getMatrix();
        Vector3f from = matrix4f.transformPosition(new Vector3f(0, 0, 0));
        Vector3f to = matrix4f.transformPosition(new Vector3f(0, 0, 1));

        Vector3f delta = new Vector3f().add(to).sub(from).mul(1, 0, -1);
        float length = delta.length();
        if (length == 0) delta = new Vector3f(0, 0, 1);
        else delta = delta.mul(1 / length);

        Quaternionf quaternion = new Quaternionf().lookAlong(delta, new Vector3f(0, 1, 0));
        return new Transformation(transformation.getTranslation(), quaternion, transformation.getScale(), null);
    }
    public static Vector2f getYawPitch(Transformation transformation) {
        Matrix4f matrix4f = transformation.getMatrix();
        Vector3f from = matrix4f.transformPosition(new Vector3f(0, 0, 0));
        Vector3f to = matrix4f.transformPosition(new Vector3f(0, 0, 1));

        Vector3f delta = new Vector3f().add(to).sub(from);

        Location location = new Location(null, 0, 0, 0).setDirection(new Vector(delta.x, delta.y, delta.z));
        return new Vector2f(location.getYaw(), location.getPitch());
    }

    public static IIndexGroup docsVectorInt(String index) {
        return JsonGroup.of(index, IJElement.join(
                IJElement.raw(1),
                IJElement.text(" "),
                IJElement.raw(2),
                IJElement.text(" "),
                IJElement.raw(3)
        ), IComment.text("Набор из 3-х целых чисел"));
    }

    public static IIndexGroup docsVector(String index) {
        return JsonGroup.of(index, IJElement.join(
                IJElement.raw(1.5),
                IJElement.text(" "),
                IJElement.raw(2.5),
                IJElement.text(" "),
                IJElement.raw(3.5)
        ), IComment.text("Набор из 3-х чисел"));
    }
    public static IIndexGroup docsLocation(String index, IIndexDocs vector) {
        return JsonEnumInfo.of(index, IComment.text("Описание положения, поворота и наклона"))
                .add(IJElement.link(vector))
                .add(IJElement.join(
                        IJElement.link(vector),
                        IJElement.text(" "),
                        IJElement.raw(22.5f),
                        IJElement.text(" "),
                        IJElement.raw(45.8f)
                ));
    }
    public static IIndexGroup docsQuaternion(String index, IIndexDocs vector) {
        return JsonEnumInfo.of(index, IComment.text("Описание поворота кветернионом"))
                .add(IJElement.anyObject(
                        JProperty.require(IName.link(vector), IJElement.raw(30.0))
                ), IComment.text("Последовательный набор поворотов вокруг оси на определенный угл"))
                .add(IJElement.anyList(
                        JObject.of(
                                JProperty.require(IName.raw("axis"), IJElement.link(vector), IComment.text("Ось")),
                                JProperty.require(IName.raw("angle"), IJElement.raw(1.5), IComment.text("Угол поворота вокруг оси"))
                        )
                ), IComment.text("Последовательный набор поворотов вокруг оси на определенный угл"))
                .add(IJElement.list(
                        IJElement.raw(0.0),
                        IJElement.raw(0.0),
                        IJElement.raw(0.0),
                        IJElement.raw(1.0)
                ), IComment.join(
                        IComment.text("Координаты "),
                        IComment.field("X"),
                        IComment.text(", "),
                        IComment.field("Y"),
                        IComment.text(", "),
                        IComment.field("Z"),
                        IComment.text(" и "),
                        IComment.field("W"),
                        IComment.text(" кватерниона")
                ))
                .add(IJElement.link(vector), IComment.text("Поворот по XYZ. ").append(IComment.warning("Не советую использовать при повороте более чем по 1 оси")));
    }
    public static IIndexGroup docsTransformation(String index, IIndexDocs vector, IIndexDocs quaternion, IIndexDocs location) {
        return JsonEnumInfo.of(index)
                .add(JObject.of(
                        JProperty.optional(IName.raw("offset"), IJElement.link(vector), IComment.text("Смещение координатной сетки")),
                        JProperty.optional(IName.raw("scale"), IJElement.link(vector), IComment.text("Увеличение координатной сетки")),
                        JProperty.optional(IName.raw("rotation"), IJElement.or(
                                JObject.of(
                                        JProperty.optional(IName.raw("left"), IJElement.link(quaternion), IComment.text("Левый поворот")),
                                        JProperty.optional(IName.raw("right"), IJElement.link(quaternion), IComment.text("Правый поворот"))
                                ),
                                IJElement.link(quaternion)
                        ), IComment.text("Поворот координатной сетки"))
                ))
                .add(IJElement.link(location));
    }
}











