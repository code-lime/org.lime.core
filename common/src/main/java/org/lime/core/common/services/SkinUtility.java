package org.lime.core.common.services;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.lime.core.common.utils.skin.SkinData;
import org.lime.core.common.utils.skin.SkinVariant;
import org.slf4j.Logger;

@Singleton
public class SkinUtility {
    private static final String API_BASE = "https://api.mineskin.org/generate/url";

    private static final int DEFAULT_RETRY = 5;

    @Inject Logger logger;
    @Inject WebUtility webUtility;

    private JsonObject getBody(String url, SkinVariant variant) {
        JsonObject json = new JsonObject();
        json.addProperty("visibility", 0);
        if (variant == SkinVariant.Auto) {
            if (url.startsWith("slim#")) {
                url = url.substring(5);
                variant = SkinVariant.Slim;
            }
            else if (url.startsWith("classic#")) {
                url = url.substring(8);
                variant = SkinVariant.Classic;
            }
        }
        if (variant != SkinVariant.Auto) json.addProperty("variant", variant.name().toLowerCase());
        json.addProperty("url", url);
        return json;
    }
    public SkinData upload(String url, SkinVariant variant) {
        return upload(url, variant, DEFAULT_RETRY);
    }
    public SkinData upload(String url) {
        return upload(url, SkinVariant.Auto, DEFAULT_RETRY);
    }

    public SkinData upload(String url, SkinVariant variant, int retry) {
        if (retry < 0)
            throw new IllegalArgumentException("Many retry");
        logger.info("Uploading {} skin with variant {}", url, variant);
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
        JsonObject json = webUtility.POST
                .create(API_BASE, getBody(url, variant).toString())
                .setHeader("User-Agent", "MineSkin-JavaClient")
                .setHeader("Content-Type", "application/json")
                .json()
                .execute()
                .val0
                .getAsJsonObject();
        if (json.has("errorCode")) {
            String error = json.get("errorCode").getAsString();
            switch (error) {
                case "rate_limit" -> {
                    long ms = json.getAsJsonObject("delayInfo").get("millis").getAsLong();
                    try {
                        logger.warn("{}! Waiting {}ms...", json.get("error").getAsString(), ms);
                        Thread.sleep(ms);
                    } catch (Exception ignored) {
                    }
                    return upload(url, variant, retry - 1);
                }
                case "Failed to find image from url" -> {
                    logger.warn("Not found image from url '{}'! Skip and get template...", url);
                    return SkinData.None;
                }
                default -> throw new IllegalArgumentException("MC ERROR: " + json);
            }
        }
        JsonObject data = json.getAsJsonObject("data").getAsJsonObject("texture");
        logger.info("Uploaded!");
        return new SkinData(data);
    }
    public SkinData upload(String url, int retry) {
        return upload(url, SkinVariant.Auto, retry);
    }
}
