package org.lime.core.common.services;

import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.lime.core.common.api.Require;
import org.lime.core.common.utils.json.builder.Json;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;
import org.lime.core.common.utils.tuple.Tuple;
import org.lime.core.common.utils.tuple.Tuple2;
import org.lime.core.common.utils.tuple.Tuple3;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public class WebUtility {
    @Inject TimerUtility timerUtility;

    public final Method DELETE = Method.of(this, HttpRequest.Builder::DELETE);
    public final Method GET = Method.of(this, HttpRequest.Builder::GET);
    public final Method PATCH = Method.of(this, (a, b) -> a.method("PATCH", b));
    public final Method POST = Method.of(this, HttpRequest.Builder::POST);
    public final Method PUT = Method.of(this, HttpRequest.Builder::PUT);

    public record Method(
            WebUtility webUtility,
            Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) {
        private static Method of(WebUtility webUtility, Func1<HttpRequest.Builder, HttpRequest.Builder> apply) {
            return of(webUtility, (a, b) -> apply.invoke(a));
        }
        private static Method of(WebUtility webUtility, Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) {
            return new Method(webUtility, apply);
        }

        public Method.Builder create(String url) {
            return create(url, HttpRequest.BodyPublishers.noBody());
        }
        public Method.Builder create(String url, String data) {
            return create(url, HttpRequest.BodyPublishers.ofString(data));
        }
        public Method.Builder create(String url, byte[] data) {
            return create(url, HttpRequest.BodyPublishers.ofByteArray(data));
        }
        public Method.Builder create(String url, HttpRequest.BodyPublisher publisher) {
            return new Method.Builder(this, apply.invoke(HttpRequest.newBuilder(toURI(url)), publisher));
        }

        private static URI toURI(String url) {
            try {
                return new URI(url);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public record Builder(
                Method method,
                HttpRequest.Builder base) {
            public Method.Builder expectContinue(boolean enable) { base.expectContinue(enable); return this; }
            public Method.Builder version(HttpClient.Version version) { base.version(version); return this; }
            public Method.Builder header(String name, String value) { base.header(name, value); return this; }
            public Method.Builder headers(String... headers) { base.headers(headers); return this; }
            public Method.Builder headers(Map<String, String> headers) { headers.forEach(this::header); return this; }
            public Method.Builder timeout(Duration duration) { base.timeout(duration); return this; }
            public Method.Builder setHeader(String name, String value) { base.setHeader(name, value); return this; }

            public <T> Method.Builder.Executor<T> custom(HttpResponse.BodyHandler<T> handler) { return of(base, handler); }
            public Method.Builder.Executor<InputStream> stream() { return custom(HttpResponse.BodyHandlers.ofInputStream()); }
            public Method.Builder.Executor<byte[]> data() { return custom(HttpResponse.BodyHandlers.ofByteArray()); }
            public Method.Builder.Executor<Void> none() { return custom(HttpResponse.BodyHandlers.discarding()); }
            public Method.Builder.Executor<String> text() { return custom(HttpResponse.BodyHandlers.ofString()); }
            public Method.Builder.Executor<Stream<String>> lines() { return custom(HttpResponse.BodyHandlers.ofLines()); }
            public Method.Builder.Executor<JsonElement> json() { return text().map(Json::parse); }

            private <T> Tuple3<T, Integer, Map<String, List<String>>> of(HttpResponse<T> response) {
                return Tuple.of(response.body(), response.statusCode(), response.headers().map());
            }
            private <T> Method.Builder.Executor<T> of(HttpRequest.Builder base, HttpResponse.BodyHandler<T> handler) {
                return new Method.Builder.Executor<>() {
                    @Override public Tuple3<T, Integer, Map<String, List<String>>> executeHeaders() {
                        HttpClient client = HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .followRedirects(HttpClient.Redirect.ALWAYS)
                                .build();
                        try {
                            return of(client.send(base.build(), handler));
                        } catch (Exception e) {
                            throw new IllegalArgumentException(e);
                        } finally {
                            try {
                                if (client instanceof AutoCloseable)
                                    ((AutoCloseable)client).close();
                            } catch (Exception e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                    }
                    @Override public CompletableFuture<Tuple3<T, Integer, Map<String, List<String>>>> executeHeadersFuture() {
                        return HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .followRedirects(HttpClient.Redirect.ALWAYS)
                                .build()
                                .sendAsync(base.build(), handler)
                                .thenApply(Method.Builder.this::of);
                    }
                    @Override public void executeHeadersAsync(Action3<T, Integer, Map<String, List<String>>> callback) {
                        method.webUtility.timerUtility.invokeAsync(this::executeHeaders, data -> callback.invoke(data.val0, data.val1, data.val2));
                    }
                };
            }

            public interface Executor<T> {
                Tuple3<T, Integer, Map<String, List<String>>> executeHeaders();
                CompletableFuture<Tuple3<T, Integer, Map<String, List<String>>>> executeHeadersFuture();
                void executeHeadersAsync(Action3<T, Integer, Map<String, List<String>>> callback);

                default Tuple2<T, Integer> execute() {
                    return executeHeaders().invokeGet((a,b,c) -> Tuple.of(a,b));
                }
                default CompletableFuture<Tuple2<T, Integer>> executeFuture() {
                    return executeHeadersFuture().thenApply(v -> v.invokeGet((a,b,c) -> Tuple.of(a,b)));
                }
                default void executeAsync(Action2<T, Integer> callback) {
                    executeHeadersAsync((a,b,c) -> callback.invoke(a,b));
                }

                default <R> Method.Builder.Executor<R> map(Func1<T, R> map) {
                    return new Method.Builder.Executor<>() {
                        @Override public Tuple3<R, Integer, Map<String, List<String>>> executeHeaders() {
                            return Method.Builder.Executor.this.executeHeaders().map(map, v -> v, v -> v);
                        }
                        @Override public CompletableFuture<Tuple3<R, Integer, Map<String, List<String>>>> executeHeadersFuture() {
                            return Method.Builder.Executor.this.executeHeadersFuture().thenApply(v -> v.map(map, vv -> vv, vv -> vv));
                        }
                        @Override public void executeHeadersAsync(Action3<R, Integer, Map<String, List<String>>> callback) {
                            Method.Builder.Executor.this.executeHeadersAsync((a, b, c) -> callback.invoke(map.invoke(a), b, c));
                        }
                    };
                }
            }
        }
    }
}
