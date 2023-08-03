package org.lime;

import com.google.gson.JsonObject;

public class skin {
    private static final String API_BASE = "https://api.mineskin.org/generate/url";

    public static class uploaded {
        public final String value;
        public final String signature;

        private uploaded(JsonObject json) {
            this.value = json.get("value").getAsString();
            this.signature = json.get("signature").getAsString();
        }
    }
    public enum Variant {
        Auto,
        Slim,
        Classic;
    }
    private static JsonObject getBody(String url, Variant variant) {
        JsonObject json = new JsonObject();
        json.addProperty("visibility", 0);
        if (variant != Variant.Auto) json.addProperty("variant", variant.name().toLowerCase());
        json.addProperty("url", url);
        return json;
    }
    public static uploaded upload(String url, Variant variant) {
        core.instance._logOP("Upload skin: " + url);
        JsonObject json =  web.method.POST
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
                case "Too many requests":
                    long ms = json.getAsJsonObject("delayInfo").get("millis").getAsLong();
                    try {
                        core.instance._logOP("Too many requests! Waiting " + ms + "ms...");
                        Thread.sleep(ms);
                    }
                    catch (Exception ignored) { }
                    return upload(url);
                default:
                    throw new IllegalArgumentException("MC ERROR: " + json.get("error").getAsString());
            }
        }
        JsonObject data = json.getAsJsonObject("data").getAsJsonObject("texture");
        core.instance._logOP("Uploaded!");
        return new uploaded(data);
    }
    public static uploaded upload(String url) {
        return upload(url, Variant.Auto);
    }
}
