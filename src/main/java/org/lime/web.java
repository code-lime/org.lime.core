package org.lime;

import com.google.gson.JsonElement;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class web {
    /*public static class server {
        public static class builder {
            public final core plugin;
            private builder(core plugin) {
                this.plugin = plugin;
            }

            public server host(int port) {
                return new server(this, port);
            }
        }

        private final core plugin;
        private final HttpServer server;
        private final system.cancel cancel;

        private final UriHttpRequestHandlerMapper handlerMapper = new UriHttpRequestHandlerMapper();

        public void addPage(String pattern, HttpRequestHandler handler) {
            handlerMapper.register(pattern, handler);
        }
        public void addPage(String pattern, ContentType type, system.Func0<String> callback) {
            addPage(pattern, (HttpRequest request, HttpResponse response, HttpContext context) -> {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity("{\"data\":\"tmp\"}", ContentType.APPLICATION_JSON));
                plugin._InvokeSync(() -> {
                });
            });
        }

        private core.element create() {
            return core.element.create(server.class)
                    .withUninit(this::uninit);
        }

        private server(builder builder, int port) {
            HttpProcessor proc = HttpProcessorBuilder.create().
                    add(new ResponseServer("MockUpdateCenter")).
                    add(new ResponseContent()).
                    add(new RequestConnControl()).
                    build();

            handlerMapper.register("/tmp.txt", (HttpRequest request, HttpResponse response, HttpContext context) -> {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity("{\"data\":\"tmp\"}", ContentType.APPLICATION_JSON));
            });

            plugin = builder.plugin;
            server = ServerBootstrap
                    .bootstrap()
                    .setListenerPort(port)
                    .setHttpProcessor(proc)
                    .setHandlerMapper(handlerMapper)
                    .setExceptionLogger(builder.plugin::_LogStackTrace)
                    .create();
            try { server.start(); }
            catch (IOException e) { throw new IllegalArgumentException(e); }
            cancel = plugin.add(create());
        }
        public void dispose() {
            cancel.invoke();
        }
        private void uninit() {
            server.shutdown(0, null);
        }
    }

    public static server.builder create(core plugin) {
        return new server.builder(plugin);
    }*/
    public static class method {
        public static final method DELETE = method.of(HttpRequest.Builder::DELETE);
        public static final method GET = method.of(HttpRequest.Builder::GET);
        public static final method PATCH = method.of((a,b) -> a.method("PATCH", b));
        public static final method POST = method.of(HttpRequest.Builder::POST);
        public static final method PUT = method.of(HttpRequest.Builder::PUT);

        private static method of(system.Func1<HttpRequest.Builder, HttpRequest.Builder> apply) { return of((a,b) -> apply.invoke(a)); }
        private static method of(system.Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) { return new method(apply); }
        private final system.Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply;
        private method(system.Func2<HttpRequest.Builder, HttpRequest.BodyPublisher, HttpRequest.Builder> apply) { this.apply = apply; }

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
            public builder timeout(Duration duration) { base.timeout(duration); return this; }
            public builder setHeader(String name, String value) { base.setHeader(name, value); return this; }

            public <T> executor<T> custom(HttpResponse.BodyHandler<T> handler) { return of(base, handler); }
            public executor<byte[]> data() { return custom(HttpResponse.BodyHandlers.ofByteArray()); }
            public executor<Void> none() { return custom(HttpResponse.BodyHandlers.discarding()); }
            public executor<String> text() { return custom(HttpResponse.BodyHandlers.ofString()); }
            public executor<JsonElement> json() { return text().map(system.json::parse); }

            private static <T>system.Toast2<T, Integer> of(HttpResponse<T> response) {
                return system.toast(response.body(), response.statusCode());
            }
            private static <T> executor<T> of(HttpRequest.Builder base, HttpResponse.BodyHandler<T> handler) {
                return new executor<>() {
                    @Override public system.Toast2<T, Integer> execute() {
                        try { return of(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build().send(base.build(), handler)); }
                        catch (Exception e) { throw new IllegalArgumentException(e); }
                    }
                    @Override public void executeAsync(system.Action2<T, Integer> callback) {
                        core.instance._invokeAsync(this::execute, data -> callback.invoke(data.val0, data.val1));
                    }
                };
            }

            public interface executor<T> {
                system.Toast2<T, Integer> execute();
                void executeAsync(system.Action2<T, Integer> callback);

                default <R> executor<R> map(system.Func1<T, R> map) {
                    return new executor<>() {
                        @Override public system.Toast2<R, Integer> execute() { return executor.this.execute().map(map, v -> v); }
                        @Override public void executeAsync(system.Action2<R, Integer> callback) { executor.this.executeAsync((k, v) -> callback.invoke(map.invoke(k), v)); }
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
                    return system.list.<String>of()
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
/*
    public static HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, byte[]> data) {
        HttpRequest.BodyPublishers.ofByteArray()
        // Result request body
        List<byte[]> byteArrays = new ArrayList<>();

        String boundary = new BigInteger(256, new Random()).toString();

        // Separator with boundary
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);

        // Iterating over data parts
        for (Map.Entry<String, byte[]> entry : data.entrySet()) {
            // Opening boundary
            byteArrays.add(separator);
            var path = (Path) entry.getValue();
            String mimeType = Files.probeContentType(path);
            byteArrays.add(("\"" + entry.getKey() + "\"; filename=\""
                    + path.getFileName() + "\"\r\nContent-Type: " + mimeType
                    + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(Files.readAllBytes(path));
            byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        }

        // Closing boundary
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

        // Serializing as byte array
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
*/

    /*public enum method {
        DELETE(url -> HttpRequest.newBuilder(toURI(url)).DELETE()),
        GET(url -> HttpRequest.newBuilder(toURI(url)).GET()),
        PATCH((url, data) -> HttpRequest.newBuilder(toURI(url)).method("PATCH", data)),
        POST((url, data) -> HttpRequest.newBuilder(toURI(url)).POST(data)),
        PUT((url, data) -> HttpRequest.newBuilder(toURI(url)).PUT(data));

        static URI toURI(String url) {
            try { return new URI(url); } catch (URISyntaxException e) { throw new IllegalArgumentException(e); }
        }

        system.Func2<String, String, HttpRequest.Builder> createBase;
        system.Func2<String, byte[], HttpRequest.Builder> createBaseBytes;
        method(system.Func2<String, HttpRequest.BodyPublisher, HttpRequest.Builder> createBase) {
            this.createBase = (url, data) -> createBase.invoke(url, HttpRequest.BodyPublishers.ofString(data));
            this.createBaseBytes = (url, data) -> createBase.invoke(url, HttpRequest.BodyPublishers.ofByteArray(data));
        }
        method(system.Func1<String, HttpRequest.Builder> createBase) { this((url, data) -> createBase.invoke(url)); }

        public HttpRequest.Builder create(String url) { return createBase.invoke(url, null); }
        public HttpRequest.Builder create(String url, String data) { return createBase.invoke(url, data); }
        public HttpRequest.Builder create(String url, byte[] data) { return createBaseBytes.invoke(url, data); }
    }*/


    /*public static system.Toast2<byte[], Integer> execute(HttpRequest.Builder base) {
        try { return of(HttpClient.newHttpClient().send(base.build(), HttpResponse.BodyHandlers.ofByteArray())); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static system.Toast2<String, Integer> executeText(HttpRequest.Builder base) {
        try { return of(HttpClient.newHttpClient().send(base.build(), HttpResponse.BodyHandlers.ofString())); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static system.Toast2<JsonElement, Integer> executeJson(HttpRequest.Builder base) {
        return executeText(base).map(system.json::parse, v -> v);
    }

    public static void executeAsync(HttpRequest.Builder base, system.Action2<byte[], Integer> callback) {
        core.instance._InvokeAsync(() -> execute(base), data -> callback.invoke(data.val0, data.val1));
    }
    public static void executeTextAsync(HttpRequest.Builder base, system.Action2<String, Integer> callback) {
        core.instance._InvokeAsync(() -> executeText(base), data -> callback.invoke(data.val0, data.val1));
    }
    public static void executeJsonAsync(HttpRequest.Builder base, system.Action2<JsonElement, Integer> callback) {
        core.instance._InvokeAsync(() -> executeJson(base), data -> callback.invoke(data.val0, data.val1));
    }*/
}

























