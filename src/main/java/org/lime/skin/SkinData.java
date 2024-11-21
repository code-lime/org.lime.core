package org.lime.skin;

import com.google.gson.JsonObject;

public class SkinData {
    private static final String noneValue = "ewogICJ0aW1lc3RhbXAiIDogMTcxMDY0MjExOTA0OSwKICAicHJvZmlsZUlkIiA6ICI1NDk1NmNlY2IzNWE0OTBkOTFhMzRjMmU5YTc2ZTk4NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJMaW1lIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI1MjU2OGJjZjY5NThmYmQ3ODU3ZmVhZjE1ZmFlZTVjOWZjMzljZjEyYzExNGRiNTBhNDMwYjE4ZGYxMzhlMTAiCiAgICB9CiAgfQp9";
    private static final String noneSignature = "eDS+I6i9kttvTjUs2fwl4YNY3jgHxKBIiXeSl9bAw8SH0iHIj39dYz9E+GtK+vYcmg3nXg9n1qaG2Z/Fy5zCc6bUr657Qytx4cgPYMq8NV2XEeoIDhEQuEpBer7Q/186F1Etv8wreEcmJ8wpqY154HTrEV8V6HYUYAOA+xf6DtgWC1ICUJGspVr1b9Ydvrw+cQuEl9Wkr012Ai/g6o7lQEPNvZTcTLCoHf1tvd12m+RqoUdSMHlsTHzNy4ayXUF6TrrYJ+Y+lBUQdHP3nj6MLspZJGCoTJEgxwxnMwc7R1d17gatnV/Z/6CPznZ+gyQuSbDgAVlHm5YMniczg5ueUz12NaKiB3wk2yOc5aFSU0OCFAsIzR+1ZiRTskBNkCksP3NDHJnTfjNTZxn/CNtW44aie+4pwwKb6lKteXygNKJu71Tslny6tBALQeyWGVMGBUPBEep/LRJ/k6OdlW13aU1qdw847yEiEw7k6PWuB/OiMg5LicnIG9ArDmEOYx4YbswGFyPgcUDPxy0blBuPfPl/3s5JpeNEj/RMQYl1aOix1/f25yzmCPoPzwCT9nAPRAfPEHfkGW8bxZjz9BXYkfaQtoFu+rSIVZS8R/PeOh94O+BAW9KcPDQh2WbhqSRgPLEqRNnGkLNs85paE/wGu5y+egmKO2A2ZsXepchGiBY=";
    public static final SkinData None = new SkinData(noneValue, noneSignature);

    public final String value;
    public final String signature;

    SkinData(JsonObject json) {
        this.value = json.get("value").getAsString();
        this.signature = json.get("signature").getAsString();
    }

    private SkinData(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }
}
