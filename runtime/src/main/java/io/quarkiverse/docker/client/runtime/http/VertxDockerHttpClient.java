package io.quarkiverse.docker.client.runtime.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.github.dockerjava.transport.DockerHttpClient;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;

public class VertxDockerHttpClient implements DockerHttpClient {

    private final Vertx vertx;
    private final HttpClient client;
    private final URI dockerHost;
    private final String pathPrefix;
    private final SocketAddress socketAddress;
    private static final int BUFFER_SIZE = 8192;

    public VertxDockerHttpClient(Vertx vertx, DockerClientConfig config) {
        this.vertx = vertx;
        this.dockerHost = URI.create(config.dockerHost());

        HttpClientOptions options = new HttpClientOptions()
                .setConnectTimeout((int) config.connectTimeout().toMillis())
                .setIdleTimeout((int) config.readTimeout().getSeconds())
                .setKeepAlive(true);

        if (config.tlsVerify() && config.certPath() != null) {
            /*
             * options.setSsl(true)
             * .setTrustAll(false)
             * .setPemTrustOptions(new PemKeyCertOptions()
             * .addCertPath(config.certPath() + "/ca.pem"))
             * .setPemKeyCertOptions(new PemKeyCertOptions()
             * .addKeyPath(config.certPath() + "/key.pem")
             * .addCertPath(config.certPath() + "/cert.pem"));
             */
        }

        switch (dockerHost.getScheme()) {
            case "unix":
                this.pathPrefix = "";
                this.socketAddress = SocketAddress.domainSocketAddress(dockerHost.getPath());
                options.setDefaultHost("localhost");
                break;
            case "npipe":
                this.pathPrefix = "";
                String pipePath = dockerHost.getPath().replace('/', '\\');
                if (!pipePath.startsWith("\\")) {
                    pipePath = "\\" + pipePath;
                }
                this.socketAddress = SocketAddress.domainSocketAddress(pipePath);
                options.setDefaultHost("localhost");
                break;
            case "tcp":
                String rawPath = dockerHost.getRawPath();
                this.pathPrefix = rawPath.endsWith("/") ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
                this.socketAddress = SocketAddress.inetSocketAddress(
                        dockerHost.getPort() != -1 ? dockerHost.getPort() : (dockerHost.getScheme().equals("https") ? 443 : 80),
                        dockerHost.getHost());
                options.setDefaultHost(dockerHost.getHost())
                        .setDefaultPort(socketAddress.port());
                break;
            default:
                throw new IllegalArgumentException("Unsupported protocol scheme: " + dockerHost);
        }

        this.client = this.vertx.createHttpClient(options);
    }

    @Override
    public Response execute(Request request) {
        CompletableFuture<Response> futureResponse = new CompletableFuture<>();

        // Create Vert.x RequestOptions
        RequestOptions options = new RequestOptions()
                .setMethod(HttpMethod.valueOf(request.method()))
                .setURI(pathPrefix + request.path())
                .setServer(socketAddress);

        // Set request headers
        request.headers().forEach(options::addHeader);

        // Handle hijacked connections for attach/exec
        if (request.hijackedInput() != null) {
            options.addHeader("Upgrade", "tcp").addHeader("Connection", "Upgrade");
        }

        // Execute the request
        client.request(options)
                .compose(httpRequest -> {
                    Future<io.vertx.core.http.HttpClientResponse> responseFuture;

                    if (request.bodyBytes() != null) {
                        responseFuture = httpRequest.send(Buffer.buffer(request.bodyBytes()));
                    } else if (request.body() != null) {
                        return sendInputStream(httpRequest, request.body()).compose(v -> httpRequest.send());
                    } else if ("POST".equals(request.method())) {
                        responseFuture = httpRequest.send(Buffer.buffer());
                    } else {
                        responseFuture = httpRequest.send();
                    }

                    if (request.hijackedInput() != null) {
                        setupHijackedInput(httpRequest, request.hijackedInput());
                    }

                    return responseFuture;
                })
                .onSuccess(response -> {
                    futureResponse.complete(new VertxResponse(response));
                })
                .onFailure(futureResponse::completeExceptionally);

        try {
            return futureResponse.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute request", e);
        }
    }

    private Future<Void> sendInputStream(io.vertx.core.http.HttpClientRequest request, InputStream inputStream) {
        return vertx.executeBlocking(promise -> {
            try {
                byte[] chunk = new byte[BUFFER_SIZE];
                int read;
                while ((read = inputStream.read(chunk)) != -1) {
                    if (read > 0) {
                        // Create buffer from the read bytes
                        byte[] data = new byte[read];
                        System.arraycopy(chunk, 0, data, 0, read);
                        request.write(Buffer.buffer(data));
                    }
                }
                promise.complete();
            } catch (IOException e) {
                promise.fail(e);
            }
        });
    }

    private void setupHijackedInput(io.vertx.core.http.HttpClientRequest request, InputStream hijackedInput) {
        request.response().onSuccess(response -> {
            vertx.executeBlocking(promise -> {
                try {
                    byte[] chunk = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = hijackedInput.read(chunk)) != -1) {
                        if (read > 0) {
                            byte[] data = new byte[read];
                            System.arraycopy(chunk, 0, data, 0, read);
                            request.write(Buffer.buffer(data));
                            response.fetch(1);
                        }
                    }
                    promise.complete();
                } catch (IOException e) {
                    promise.fail(e);
                }
            });
        });
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    private static class VertxResponse implements Response {
        private final HttpClientResponse response;
        private Buffer body;

        VertxResponse(HttpClientResponse response) {
            this.response = response;
            this.body = Buffer.buffer();
            response.handler(buffer -> {
                this.body.appendBuffer(buffer);
            });
        }

        @Override
        public int getStatusCode() {
            return response.statusCode();
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return new HashMap<>(response.headers().entries().stream()
                    .collect(HashMap::new, (m, e) -> m.computeIfAbsent(e.getKey(), k -> List.of())
                            .add(e.getValue()), HashMap::putAll));
        }

        @Override
        public String getHeader(String name) {
            return response.getHeader(name);
        }

        @Override
        public InputStream getBody() {
            return body != null ? new ByteArrayInputStream(body.getBytes()) : null;
        }

        @Override
        public void close() {
            // No need to explicitly close in Vert.x
        }
    }
}
