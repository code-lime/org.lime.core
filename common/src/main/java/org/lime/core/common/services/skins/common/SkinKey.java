package org.lime.core.common.services.skins.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public record SkinKey(
        String url,
        SkinVariant variant) {
    public static String toUrl(byte[] image) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
    }

    public static SkinKey of(
            String url,
            SkinVariant variant) {
        return new SkinKey(url, variant);
    }
    public static SkinKey of(
            byte[] image,
            SkinVariant variant) {
        return of(toUrl(image), variant);
    }
    public static SkinKey of(
            BufferedImage image,
            SkinVariant variant) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return of(out.toByteArray(), variant);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
