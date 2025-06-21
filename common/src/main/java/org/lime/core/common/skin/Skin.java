package org.lime.core.common.skin;

import com.google.gson.JsonObject;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.Web;

public class Skin {
    private static final String API_BASE = "https://api.mineskin.org/generate/url";

    private static final int DEFAULT_RETRY = 5;

    private static JsonObject getBody(String url, SkinVariant variant) {
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
    public static SkinData upload(String url, SkinVariant variant) {
        return upload(url, DEFAULT_RETRY);
    }
    public static SkinData upload(String url) {
        return upload(url, SkinVariant.Auto, DEFAULT_RETRY);
    }

    public static SkinData upload(String url, SkinVariant variant, int retry) {
        if (retry < 0)
            throw new IllegalArgumentException("Many retry");
        BaseCoreInstance.global.$logOP("Upload skin: " + url);
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
        JsonObject json =  Web.Method.POST
                .create(API_BASE, getBody(url, variant).toString())
                .setHeader("User-Agent", "MineSkin-JavaClient")
                .setHeader("Content-Type", "application/json")
                .json()
                .execute()
                .val0
                .getAsJsonObject();
        if (json.has("error")) {
            String error = json.get("error").getAsString();
            switch (error) {
                case "Too many requests" -> {
                    long ms = json.getAsJsonObject("delayInfo").get("millis").getAsLong();
                    try {
                        BaseCoreInstance.global.$logOP("Too many requests! Waiting " + ms + "ms...");
                        Thread.sleep(ms);
                    } catch (Exception ignored) {
                    }
                    return upload(url, variant, retry - 1);
                }
                case "Failed to find image from url" -> {
                    BaseCoreInstance.global.$logOP("Not found image from url '" + url + "'! Skip and get template...");
                    return SkinData.None;
                }
                default -> throw new IllegalArgumentException("MC ERROR: " + json.get("error").getAsString());
            }
        }
        JsonObject data = json.getAsJsonObject("data").getAsJsonObject("texture");
        BaseCoreInstance.global.$logOP("Uploaded!");
        return new SkinData(data);
    }
    public static SkinData upload(String url, int retry) {
        return upload(url, SkinVariant.Auto, retry);
    }
}
