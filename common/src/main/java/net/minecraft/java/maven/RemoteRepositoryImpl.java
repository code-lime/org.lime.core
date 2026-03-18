package net.minecraft.java.maven;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record RemoteRepositoryImpl(
        String id,
        String type,
        String url,
        @Nullable PolicyImpl snapshotPolicy,
        @Nullable PolicyImpl releasePolicy,
        @Nullable AuthenticationImpl auth,
        @Nullable ProxyImpl proxy) {
    public static final String DEFAULT_TYPE = "default";

    public record PolicyImpl(
            boolean enable,
            @Nullable String updatePolicy,
            @Nullable String checksumPolicy) {
        public static PolicyImpl of(Object value) {
            return asMap(value)
                    .map(map -> {
                        boolean enable = asNullBoolean(map.get("enable")).orElse(true);
                        @Nullable String updatePolicy = asNullString(map.get("update")).orElse(null);
                        @Nullable String checksumPolicy = asNullString(map.get("checksum")).orElse(null);
                        return new PolicyImpl(enable, updatePolicy, checksumPolicy);
                    })
                    .orElseGet(() -> new PolicyImpl(asBoolean(value), null, null));
        }
    }
    public record AuthenticationImpl(
            String username,
            String password) {
        public static AuthenticationImpl of(Object value) {
            Map<?, ?> dat = asMap(value).orElseThrow();

            return new AuthenticationImpl(
                    asString(dat.get("username")),
                    asString(dat.get("password"))
            );
        }
    }
    public record ProxyImpl(
            String protocol,
            String host,
            int port,
            @Nullable AuthenticationImpl auth) {
        public static ProxyImpl of(Object value) {
            Map<?, ?> dat = asMap(value).orElseThrow();

            String host = asString(dat.get("host"));
            String protocol = asNullString(dat.get("protocol")).orElse("http");
            int port = asInteger(dat.get("port"));
            AuthenticationImpl auth = Optional.ofNullable(dat.get("auth"))
                    .map(AuthenticationImpl::of)
                    .orElse(null);

            return new ProxyImpl(protocol, host, port, auth);
        }
    }

    private static Optional<Map<?,?>> asMap(Object value) {
        if (value instanceof Map<?,?> map)
            return Optional.of( map);
        else if (value instanceof JsonObject json)
            return Optional.of(json.asMap());
        else
            return Optional.empty();
    }
    private static String asString(Object value) {
        if (value instanceof JsonElement json)
            return json.getAsString();
        return value.toString();
    }
    private static boolean asBoolean(Object value) {
        String str = asString(value);
        return switch (str.toLowerCase()) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("'" + str + "' can be only 'true' or 'false'");
        };
    }
    private static int asInteger(Object value) {
        return Integer.parseInt(asString(value));
    }
    private static Optional<Boolean> asNullBoolean(Object value) {
        return asNullString(value)
                .map(str -> switch (str.toLowerCase()) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw new IllegalArgumentException("'" + str + "' can be only 'true' or 'false'");
                });
    }
    private static Optional<String> asNullString(Object value) {
        if (value == null)
            return Optional.empty();
        if (value instanceof JsonElement json)
            return json.isJsonNull() ? Optional.empty() : Optional.of(json.getAsString());
        return Optional.of(Objects.toString(value));
    }

    public static RemoteRepositoryImpl of(String id, Object value) {
        return asMap(value)
                .map(map -> {
                    @Nullable PolicyImpl snapshotPolicy = null;
                    @Nullable PolicyImpl releasePolicy = null;
                    @Nullable AuthenticationImpl auth = null;
                    @Nullable ProxyImpl proxy = null;

                    String url = asString(map.get("url"));
                    for (var kv : map.entrySet()) {
                        switch (asString(kv.getKey())) {
                            case "snapshot" -> snapshotPolicy = PolicyImpl.of(kv.getValue());
                            case "release" -> releasePolicy = PolicyImpl.of(kv.getValue());
                            case "auth" -> auth = AuthenticationImpl.of(kv.getValue());
                            case "proxy" -> proxy = ProxyImpl.of(kv.getValue());
                        }
                    }

                    return new RemoteRepositoryImpl(id, DEFAULT_TYPE, url, snapshotPolicy, releasePolicy, auth, proxy);
                })
                .orElseGet(() -> new RemoteRepositoryImpl(id, DEFAULT_TYPE, asString(value), null, null, null, null));
    }

    @Override
    public @NotNull String toString() {
        return id() + "::" + url();
    }
}
