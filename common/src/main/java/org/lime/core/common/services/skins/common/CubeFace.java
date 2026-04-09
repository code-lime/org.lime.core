package org.lime.core.common.services.skins.common;

import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3ic;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public enum CubeFace {
    TOP(CubeDirection.UP, Color.GRAY, (uv, s) -> uv.add(s.z(), 0), s -> new Vector2i(s.x(), s.z())),
    BOTTOM(CubeDirection.DOWN, Color.DARK_GRAY, (uv, s) -> uv.add(s.z() + s.x(), 0), s -> new Vector2i(s.x(), s.z())),

    RIGHT(CubeDirection.WEST, Color.RED, (uv, s) -> uv.add(s.z() + s.x(), s.z()), s -> new Vector2i(s.z(), s.y())),
    FRONT(CubeDirection.SOUTH, Color.BLUE, (uv, s) -> uv.add(s.z(), s.z()), s -> new Vector2i(s.x(), s.y())),
    LEFT(CubeDirection.EAST, Color.GREEN, (uv, s) -> uv.add(0, s.z()), s -> new Vector2i(s.z(), s.y())),
    BACK(CubeDirection.NORTH, Color.YELLOW, (uv, s) -> uv.add(s.z() + s.x() + s.z(), s.z()), s -> new Vector2i(s.x(), s.y())),

    ;

    private final CubeDirection direction;
    private final Color color;
    private final BiConsumer<Vector2i, Vector3ic> appendOffset;
    private final Function<Vector3ic, Vector2i> extractSize;

    CubeFace(CubeDirection direction, Color color, BiConsumer<Vector2i, Vector3ic> appendOffset, Function<Vector3ic, Vector2i> extractSize) {
        this.direction = direction;
        this.color = color;
        this.appendOffset = appendOffset;
        this.extractSize = extractSize;
    }

    public CubeDirection direction() {
        return direction;
    }

    public Vector2i computePosition(Vector2i uv, Vector3ic size) {
        Vector2i pos = new Vector2i(uv);
        appendOffset.accept(pos, size);
        return pos;
    }
    public Vector2i computeSize(Vector3ic size) {
        return extractSize.apply(size);
    }
    public Rectangle computeRectangle(SkinPart part, SkinLayer layer) {
        return computeRectangle(part.point(layer), part.size);
    }
    public Rectangle computeRectangle(Vector2ic uv, Vector3ic size) {
        return new Rectangle(computePosition(new Vector2i(uv), size), computeSize(size));
    }

    public void writeTemplateFace(
            Graphics2D g,
            SkinPart part,
            SkinLayer layer) {
        Vector2ic uv = part.point(layer);
        Vector3ic s = part.size;

        Vector2i pos = computePosition(new Vector2i(uv), s);
        Vector2i size = computeSize(s);

        g.setColor(color);
        g.fillRect(pos.x(), pos.y(), size.x(), size.y());
        if (layer == SkinLayer.OVERLAY) {
            g.clearRect(pos.x() + 1, pos.y() + 1, size.x() - 2, size.y() - 2);
        }
    }
    public void clearFace(
            Graphics2D g,
            SkinPart part,
            SkinLayer layer) {
        Vector2ic uv = part.point(layer);
        Vector3ic s = part.size;

        Vector2i pos = computePosition(new Vector2i(uv), s);
        Vector2i size = computeSize(s);

        g.clearRect(pos.x(), pos.y(), size.x(), size.y());
    }
}
