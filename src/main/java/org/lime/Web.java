package org.lime;

import com.google.gson.JsonElement;
import org.lime.system.execute.*;
import org.lime.json.builder.Json;
import org.lime.system.ListBuilder;
import org.lime.system.tuple.*;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Web {
    public static class Method {
        public static final Method DELETE = Method.of(HttpRequest.Builder::DELETE);
        public static final Method GET = Method.of(HttpRequest.Builder::GET);
        public static final Method PATCH = Method.of((a, b) -> a.method("PATCH", b));
        public static final Method POST = Method.of(HttpRequest.Builder::POST);
        public static final Method PUT = Method.of(HttpRequest.Builder::PUT);

        private static Method of(Func1<HttpRequest.Builder, HttpRequest.Builder> apply) { return of((a, b) -> apply.invoke(a)); }
        private static Method of(Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) { return new Method(apply); }
        private final Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply;
        private Method(Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) { this.apply = apply; }

        public Builder create(String url) { return create(url, HttpRequest.BodyPublishers.noBody()); }
        public Builder create(String url, String data) { return create(url, HttpRequest.BodyPublishers.ofString(data)); }
        public Builder create(String url, byte[] data) { return create(url, HttpRequest.BodyPublishers.ofByteArray(data)); }
        public Builder create(String url, HttpRequest.BodyPublisher publisher) { return new Builder(apply.invoke(HttpRequest.newBuilder(toURI(url)), publisher)); }

        private static URI toURI(String url) { try { return new URI(url); } catch (URISyntaxException e) { throw new IllegalArgumentException(e); } }

        public static class Builder {
            public final HttpRequest.Builder base;
            private Builder(HttpRequest.Builder base) { this.base = base; }

            public Builder expectContinue(boolean enable) { base.expectContinue(enable); return this; }
            public Builder version(HttpClient.Version version) { base.version(version); return this; }
            public Builder header(String name, String value) { base.header(name, value); return this; }
            public Builder headers(String... headers) { base.headers(headers); return this; }
            public Builder headers(Map<String, String> headers) { headers.forEach(this::header); return this; }
            public Builder timeout(Duration duration) { base.timeout(duration); return this; }
            public Builder setHeader(String name, String value) { base.setHeader(name, value); return this; }

            public <T> Executor<T> custom(HttpResponse.BodyHandler<T> handler) { return of(base, handler); }
            public Executor<InputStream> stream() { return custom(HttpResponse.BodyHandlers.ofInputStream()); }
            public Executor<byte[]> data() { return custom(HttpResponse.BodyHandlers.ofByteArray()); }
            public Executor<Void> none() { return custom(HttpResponse.BodyHandlers.discarding()); }
            public Executor<String> text() { return custom(HttpResponse.BodyHandlers.ofString()); }
            public Executor<Stream<String>> lines() { return custom(HttpResponse.BodyHandlers.ofLines()); }
            public Executor<JsonElement> json() { return text().map(Json::parse); }

            private static <T> Tuple3<T, Integer, Map<String, List<String>>> of(HttpResponse<T> response) {
                return Tuple.of(response.body(), response.statusCode(), response.headers().map());
            }
            private static <T> Executor<T> of(HttpRequest.Builder base, HttpResponse.BodyHandler<T> handler) {
                return new Executor<>() {
                    @Override public Tuple3<T, Integer, Map<String, List<String>>> executeHeaders() {
                        try (HttpClient client = HttpClient.newBuilder()
                                    .version(Version.HTTP_1_1)
                                    .followRedirects(HttpClient.Redirect.ALWAYS)
                                    .build()) {
                            return of(client.send(base.build(), handler));
                        }
                        catch (Exception e) { throw new IllegalArgumentException(e); }
                    }
                    @Override public void executeHeadersAsync(Action3<T, Integer, Map<String, List<String>>> callback) {
                        LimeCore.instance._invokeAsync(this::executeHeaders, data -> callback.invoke(data.val0, data.val1, data.val2));
                    }
                };
            }

            public interface Executor<T> {
                Tuple3<T, Integer, Map<String, List<String>>> executeHeaders();
                void executeHeadersAsync(Action3<T, Integer, Map<String, List<String>>> callback);

                default Tuple2<T, Integer> execute() {
                    return executeHeaders().invokeGet((a,b,c) -> Tuple.of(a,b));
                }
                default void executeAsync(Action2<T, Integer> callback) {
                    executeHeadersAsync((a,b,c) -> callback.invoke(a,b));
                }

                default <R> Executor<R> map(Func1<T, R> map) {
                    return new Executor<>() {
                        @Override public Tuple3<R, Integer, Map<String, List<String>>> executeHeaders() {
                            return Executor.this.executeHeaders().map(map, v -> v, v -> v);
                        }
                        @Override public void executeHeadersAsync(Action3<R, Integer, Map<String, List<String>>> callback) {
                            Executor.this.executeHeadersAsync((a, b, c) -> callback.invoke(map.invoke(a), b, c));
                        }
                    };
                }
            }
        }
    }

    public static class Multipart {
        public static String boundary() {
            return "-------------" + new BigInteger(256, new Random()).toString();
        }
        public static String contentType(String boundary) {
            return "multipart/form-data; boundary=" + boundary;
        }

        public static class Builder {
            private static final String RN = "\r\n";
            private static final byte[] RN_BYTES = RN.getBytes(StandardCharsets.UTF_8);

            private record Mime(HttpRequest.BodyPublisher publisher, Map<String, String> headers) {
                private List<String> _head(String key, String boundary) {
                    return ListBuilder.<String>of()
                            .add("--" + boundary)
                            .add("Content-Disposition: form-data; name=\"" + key + "\"")
                            .add(headers.entrySet(), kv -> kv.getKey() + ": " + kv.getValue())
                            .add("", "")
                            .build();
                }
                public HttpRequest.BodyPublisher head(String key, String boundary) {
                    return HttpRequest.BodyPublishers.ofByteArray(
                            String.join(RN, _head(key, boundary))
                                    .getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
            private final LinkedHashMap<String, Mime> mimes = new LinkedHashMap<>();
            private Builder() { }

            public Builder add(String key, HttpRequest.BodyPublisher publisher) {
                return begin(key, publisher).end();
            }
            public _0 begin(String key, HttpRequest.BodyPublisher publisher) {
                return new _0(key, publisher);
            }
            public class _0 {
                private final String key;
                private final HttpRequest.BodyPublisher publisher;
                private final HashMap<String, String> headers = new HashMap<String, String>();

                private _0(String key, HttpRequest.BodyPublisher publisher) {
                    this.key = key;
                    this.publisher = publisher;
                }

                public _0 header(String name, String value) { headers.put(name, value); return this; }

                public Builder end() {
                    Builder.this.mimes.put(key, new Mime(publisher, headers));
                    return Builder.this;
                }
            }

            public HttpRequest.BodyPublisher build(String boundary) {
                List<byte[]> byteArrays = new ArrayList<>();

                List<HttpRequest.BodyPublisher> publishers = new ArrayList<>();

                mimes.forEach((key, value) -> {
                    publishers.add(value.head(key, boundary));
                    publishers.add(value.publisher);
                    publishers.add(HttpRequest.BodyPublishers.ofByteArray(RN_BYTES));
                });
                publishers.add(HttpRequest.BodyPublishers.ofByteArray(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8)));
                return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);

            }
        }
        public static Builder create() { return new Builder(); }
    }
}

























