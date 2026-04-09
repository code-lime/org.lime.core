package org.lime.core.common.services.skins.common;

import com.google.gson.JsonObject;
import org.lime.core.common.utils.json.builder.Json;

public record SkinData(
        String value,
        String signature) {
    private static final String NONE_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTcxMDY0MjExOTA0OSwKICAicHJvZmlsZUlkIiA6ICI1NDk1NmNlY2IzNWE0OTBkOTFhMzRjMmU5YTc2ZTk4NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJMaW1lIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI1MjU2OGJjZjY5NThmYmQ3ODU3ZmVhZjE1ZmFlZTVjOWZjMzljZjEyYzExNGRiNTBhNDMwYjE4ZGYxMzhlMTAiCiAgICB9CiAgfQp9";
    private static final String NONE_SIGNATURE = "eDS+I6i9kttvTjUs2fwl4YNY3jgHxKBIiXeSl9bAw8SH0iHIj39dYz9E+GtK+vYcmg3nXg9n1qaG2Z/Fy5zCc6bUr657Qytx4cgPYMq8NV2XEeoIDhEQuEpBer7Q/186F1Etv8wreEcmJ8wpqY154HTrEV8V6HYUYAOA+xf6DtgWC1ICUJGspVr1b9Ydvrw+cQuEl9Wkr012Ai/g6o7lQEPNvZTcTLCoHf1tvd12m+RqoUdSMHlsTHzNy4ayXUF6TrrYJ+Y+lBUQdHP3nj6MLspZJGCoTJEgxwxnMwc7R1d17gatnV/Z/6CPznZ+gyQuSbDgAVlHm5YMniczg5ueUz12NaKiB3wk2yOc5aFSU0OCFAsIzR+1ZiRTskBNkCksP3NDHJnTfjNTZxn/CNtW44aie+4pwwKb6lKteXygNKJu71Tslny6tBALQeyWGVMGBUPBEep/LRJ/k6OdlW13aU1qdw847yEiEw7k6PWuB/OiMg5LicnIG9ArDmEOYx4YbswGFyPgcUDPxy0blBuPfPl/3s5JpeNEj/RMQYl1aOix1/f25yzmCPoPzwCT9nAPRAfPEHfkGW8bxZjz9BXYkfaQtoFu+rSIVZS8R/PeOh94O+BAW9KcPDQh2WbhqSRgPLEqRNnGkLNs85paE/wGu5y+egmKO2A2ZsXepchGiBY=";

    public static final SkinData NONE = new SkinData(NONE_VALUE, NONE_SIGNATURE);

    public JsonObject toJson() {
        return Json.object()
                .add("value", value)
                .add("signature", signature)
                .build();
    }

    public static SkinData none() {
        return NONE;
    }

    public static SkinData parse(JsonObject json) {
        return new SkinData(
                json.get("value").getAsString(),
                json.get("signature").getAsString());
    }
}
