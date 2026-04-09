package org.lime.core.common.services.skins.common;

import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.awt.*;
import java.awt.image.BufferedImage;

public record Rectangle(
        Vector2ic point,
        Vector2ic size) {
    public Rectangle offset(Vector2ic pointOffset, Vector2ic sizeOffset) {
        return new Rectangle(point.add(pointOffset, new Vector2i()), size.add(sizeOffset, new Vector2i()));
    }
    public Rectangle offsetPoint(Vector2ic pointOffset) {
        return new Rectangle(point.add(pointOffset, new Vector2i()), size);
    }
    public Rectangle offsetSize(Vector2ic sizeOffset) {
        return new Rectangle(point, size.add(sizeOffset, new Vector2i()));
    }

    public static void copyAreaScaled(
            BufferedImage src,
            BufferedImage dst,
            Rectangle srcRect,
            Rectangle dstRect) {
        Graphics2D g = dst.createGraphics();
        copyAreaScaled(g, src, srcRect, dstRect);
        g.dispose();
    }
    public static void copyAreaScaled(
            Graphics2D g,
            BufferedImage src,
            Rectangle srcRect,
            Rectangle dstRect) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int sx1 = srcRect.point().x();
        int sy1 = srcRect.point().y();
        int sx2 = sx1 + srcRect.size().x();
        int sy2 = sy1 + srcRect.size().y();

        int dx1 = dstRect.point().x();
        int dy1 = dstRect.point().y();
        int dx2 = dx1 + dstRect.size().x();
        int dy2 = dy1 + dstRect.size().y();

        g.drawImage(
                src,
                dx1, dy1, dx2, dy2,   // to (with scale)
                sx1, sy1, sx2, sy2,   // from
                null);
    }
}
