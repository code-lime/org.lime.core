package org.lime;

import com.google.gson.JsonElement;
import org.lime.system.execute.*;
import org.lime.system.json;
import org.lime.system.list;
import org.lime.system.toast.*;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class web {
    public static class method {
        public static final method DELETE = method.of(HttpRequest.Builder::DELETE);
        public static final method GET = method.of(HttpRequest.Builder::GET);
        public static final method PATCH = method.of((a,b) -> a.method("PATCH", b));
        public static final method POST = method.of(HttpRequest.Builder::POST);
        public static final method PUT = method.of(HttpRequest.Builder::PUT);

        private static method of(Func1<HttpRequest.Builder, HttpRequest.Builder> apply) { return of((a, b) -> apply.invoke(a)); }
        private static method of(Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) { return new method(apply); }
        private final Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply;
        private method(Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) { this.apply = apply; }

        public builder create(String url) { return create(url, HttpRequest.BodyPublishers.noBody()); }
        public builder create(String url, String data) { return create(url, HttpRequest.BodyPublishers.ofString(data)); }
        public builder create(String url, byte[] data) { return create(url, HttpRequest.BodyPublishers.ofByteArray(data)); }
        public builder create(String url, HttpRequest.BodyPublisher publisher) { return new builder(apply.invoke(HttpRequest.newBuilder(toURI(url)), publisher)); }

        private static URI toURI(String url) { try { return new URI(url); } catch (URISyntaxException e) { throw new IllegalArgumentException(e); } }

        public static class builder {
            public final HttpRequest.Builder base;
            private builder(HttpRequest.Builder base) { this.base = base; }

            public builder expectContinue(boolean enable) { base.expectContinue(enable); return this; }
            public builder version(HttpClient.Version version) { base.version(version); return this; }
            public builder header(String name, String value) { base.header(name, value); return this; }
            public builder headers(String... headers) { base.headers(headers); return this; }
            public builder headers(Map<String, String> headers) { headers.forEach(this::header); return this; }
            public builder timeout(Duration duration) { base.timeout(duration); return this; }
            public builder setHeader(String name, String value) { base.setHeader(name, value); return this; }

            public <T> executor<T> custom(HttpResponse.BodyHandler<T> handler) { return of(base, handler); }
            public executor<InputStream> stream() { return custom(HttpResponse.BodyHandlers.ofInputStream()); }
            public executor<byte[]> data() { return custom(HttpResponse.BodyHandlers.ofByteArray()); }
            public executor<Void> none() { return custom(HttpResponse.BodyHandlers.discarding()); }
            public executor<String> text() { return custom(HttpResponse.BodyHandlers.ofString()); }
            public executor<Stream<String>> lines() { return custom(HttpResponse.BodyHandlers.ofLines()); }
            public executor<JsonElement> json() { return text().map(json::parse); }

            private static <T> Toast3<T, Integer, Map<String, List<String>>> of(HttpResponse<T> response) {
                return Toast.of(response.body(), response.statusCode(), response.headers().map());
            }
            private static <T> executor<T> of(HttpRequest.Builder base, HttpResponse.BodyHandler<T> handler) {
                return new executor<>() {
                    @Override public Toast3<T, Integer, Map<String, List<String>>> executeHeaders() {
                        try {
                            return of(HttpClient.newBuilder()
                                .version(Version.HTTP_1_1)
                                .followRedirects(HttpClient.Redirect.ALWAYS)
                                .build()
                                .send(base.build(), handler));
                        }
                        catch (Exception e) { throw new IllegalArgumentException(e); }
                    }
                    @Override public void executeHeadersAsync(Action3<T, Integer, Map<String, List<String>>> callback) {
                        core.instance._invokeAsync(this::executeHeaders, data -> callback.invoke(data.val0, data.val1, data.val2));
                    }
                };
            }

            public interface executor<T> {
                Toast3<T, Integer, Map<String, List<String>>> executeHeaders();
                void executeHeadersAsync(Action3<T, Integer, Map<String, List<String>>> callback);

                default Toast2<T, Integer> execute() {
                    return executeHeaders().invokeGet((a,b,c) -> Toast.of(a,b));
                }
                default void executeAsync(Action2<T, Integer> callback) {
                    executeHeadersAsync((a,b,c) -> callback.invoke(a,b));
                }

                default <R> executor<R> map(Func1<T, R> map) {
                    return new executor<>() {
                        @Override public Toast3<R, Integer, Map<String, List<String>>> executeHeaders() {
                            return executor.this.executeHeaders().map(map, v -> v, v -> v);
                        }
                        @Override public void executeHeadersAsync(Action3<R, Integer, Map<String, List<String>>> callback) {
                            executor.this.executeHeadersAsync((a,b,c) -> callback.invoke(map.invoke(a), b, c));
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
            private static class Mime {
                public final HttpRequest.BodyPublisher publisher;
                public final Map<String, String> headers;

                public Mime(HttpRequest.BodyPublisher publisher, Map<String, String> headers) {
                    this.publisher = publisher;
                    this.headers = headers;
                }

                private List<String> _head(String key, String boundary) {
                    return list.<String>of()
                            .add("--" + boundary)
                            .add("Content-Disposition: form-data; name=\""+key+"\"")
                            .add(headers.entrySet(), kv -> kv.getKey() + ": " + kv.getValue())
                            .add("","")
                            .build();
                }
                public HttpRequest.BodyPublisher head(String key, String boundary) {
                    return HttpRequest.BodyPublishers.ofByteArray(
                            _head(key, boundary)
                                    .stream()
                                    .collect(Collectors.joining(RN))
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

























