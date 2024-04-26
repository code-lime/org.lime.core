package org.lime;

import com.google.gson.JsonObject;

public class skin {
    private static final String API_BASE = "https://api.mineskin.org/generate/url";

    public static class uploaded {
        private static final String noneValue = "ewogICJ0aW1lc3RhbXAiIDogMTcxMDY0MjExOTA0OSwKICAicHJvZmlsZUlkIiA6ICI1NDk1NmNlY2IzNWE0OTBkOTFhMzRjMmU5YTc2ZTk4NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJMaW1lIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI1MjU2OGJjZjY5NThmYmQ3ODU3ZmVhZjE1ZmFlZTVjOWZjMzljZjEyYzExNGRiNTBhNDMwYjE4ZGYxMzhlMTAiCiAgICB9CiAgfQp9";
        private static final String noneSignature = "eDS+I6i9kttvTjUs2fwl4YNY3jgHxKBIiXeSl9bAw8SH0iHIj39dYz9E+GtK+vYcmg3nXg9n1qaG2Z/Fy5zCc6bUr657Qytx4cgPYMq8NV2XEeoIDhEQuEpBer7Q/186F1Etv8wreEcmJ8wpqY154HTrEV8V6HYUYAOA+xf6DtgWC1ICUJGspVr1b9Ydvrw+cQuEl9Wkr012Ai/g6o7lQEPNvZTcTLCoHf1tvd12m+RqoUdSMHlsTHzNy4ayXUF6TrrYJ+Y+lBUQdHP3nj6MLspZJGCoTJEgxwxnMwc7R1d17gatnV/Z/6CPznZ+gyQuSbDgAVlHm5YMniczg5ueUz12NaKiB3wk2yOc5aFSU0OCFAsIzR+1ZiRTskBNkCksP3NDHJnTfjNTZxn/CNtW44aie+4pwwKb6lKteXygNKJu71Tslny6tBALQeyWGVMGBUPBEep/LRJ/k6OdlW13aU1qdw847yEiEw7k6PWuB/OiMg5LicnIG9ArDmEOYx4YbswGFyPgcUDPxy0blBuPfPl/3s5JpeNEj/RMQYl1aOix1/f25yzmCPoPzwCT9nAPRAfPEHfkGW8bxZjz9BXYkfaQtoFu+rSIVZS8R/PeOh94O+BAW9KcPDQh2WbhqSRgPLEqRNnGkLNs85paE/wGu5y+egmKO2A2ZsXepchGiBY=";
        public static final uploaded None = new uploaded(noneValue, noneSignature);

        public final String value;
        public final String signature;

        private uploaded(JsonObject json) {
            this.value = json.get("value").getAsString();
            this.signature = json.get("signature").getAsString();
        }
        private uploaded(String value, String signature) {
            this.value = value;
            this.signature = signature;
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
        if (variant == Variant.Auto) {
            if (url.startsWith("slim#")) {
                url = url.substring(5);
                variant = Variant.Slim;
            }
            else if (url.startsWith("classic#")) {
                url = url.substring(8);
                variant = Variant.Classic;
            }
        }
        if (variant != Variant.Auto) json.addProperty("variant", variant.name().toLowerCase());
        json.addProperty("url", url);
        return json;
    }
    public static uploaded upload(String url, Variant variant) {
        core.instance._logOP("Upload skin: " + url);
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
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
                case "Too many requests" -> {
                    long ms = json.getAsJsonObject("delayInfo").get("millis").getAsLong();
                    try {
                        core.instance._logOP("Too many requests! Waiting " + ms + "ms...");
                        Thread.sleep(ms);
                    } catch (Exception ignored) {
                    }
                    return upload(url);
                }
                case "Failed to find image from url" -> {
                    core.instance._logOP("Not found image from url '" + url + "'! Skip and get template...");
                    return uploaded.None;
                }
                default -> throw new IllegalArgumentException("MC ERROR: " + json.get("error").getAsString());
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
