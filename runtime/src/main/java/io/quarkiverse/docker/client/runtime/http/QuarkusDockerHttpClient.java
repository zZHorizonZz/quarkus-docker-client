package io.quarkiverse.docker.client.runtime.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.transport.DockerHttpClient;

import io.quarkus.runtime.annotations.RecordableConstructor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Quarkus implementation of Docker HTTP client using Vert.x.
 */
public class QuarkusDockerHttpClient implements DockerHttpClient {

    private final WebClient webClient;
    private final String dockerHost;
    private final Vertx vertx;
    private final SocketAddress socketAddress;

    @RecordableConstructor
    public QuarkusDockerHttpClient(DockerClientConfig config, Vertx vertx) {
        this.vertx = vertx;
        this.dockerHost = config.dockerHost();
        this.socketAddress = createSocketAddress();
        this.webClient = createWebClient(config);
    }

    private SocketAddress createSocketAddress() {
        if (isUnixSocket()) {
            return SocketAddress.domainSocketAddress(getUnixSocketPath());
        } else {
            String host = dockerHost.split("://")[1].split(":")[0];
            int port = Integer.parseInt(dockerHost.split(":")[2]);
            return SocketAddress.inetSocketAddress(port, host);
        }
    }

    private WebClient createWebClient(DockerClientConfig config) {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout((int) config.connectTimeout().toMillis())
                .setIdleTimeout((int) config.readTimeout().toMillis())
                .setFollowRedirects(true)
                .setTrustAll(true);

        if (socketAddress != null) {
            options.setDefaultHost(socketAddress.path());
        }

        return WebClient.create(vertx, options);
    }

    private boolean isUnixSocket() {
        return dockerHost.startsWith("unix://");
    }

    private String getUnixSocketPath() {
        return dockerHost.substring(7); // Remove "unix://"
    }

    @Override
    public Response execute(Request request) {
        HttpRequest<Buffer> clientRequest = prepareRequest(request);

        try {
            HttpResponse<Buffer> response = sendRequest(clientRequest, request).await().indefinitely();

            return new QuarkusDockerResponse(response);
        } catch (Exception e) {
            throw new DockerClientException("Failed to execute request", e);
        }
    }

    private HttpRequest<Buffer> prepareRequest(Request request) {
        String path = isUnixSocket() ? "http:///var/run/docker.sock" + request.path() : dockerHost + request.path();

        HttpRequest<Buffer> clientRequest = webClient.requestAbs(HttpMethod.valueOf(request.method()), path);

        // Add headers
        request.headers().forEach(clientRequest::putHeader);

        return clientRequest;
    }

    private Uni<HttpResponse<Buffer>> sendRequest(
            HttpRequest<Buffer> clientRequest,
            Request request) {

        if (request.body() != null) {
            Future<HttpResponse<Buffer>> responseFuture = clientRequest.sendBuffer(Buffer.buffer(request.bodyBytes()));
            return Uni.createFrom().completionStage(responseFuture.toCompletionStage());
        }

        return Uni.createFrom().completionStage(clientRequest.send().toCompletionStage());
    }

    @Override
    public void close() {
        webClient.close();
    }

    private static class QuarkusDockerResponse implements Response {
        private final int statusCode;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        QuarkusDockerResponse(HttpResponse<Buffer> response) {
            this.statusCode = response.statusCode();
            this.headers = new ConcurrentHashMap<>();

            response.headers().names().forEach(name -> {
                List<String> values = new ArrayList<>(response.headers().getAll(name));
                headers.put(name, Collections.unmodifiableList(values));
            });

            Buffer buffer = response.body();
            this.body = buffer != null ? buffer.getBytes() : new byte[0];
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void close() {
            // No resources to close in this implementation
        }
    }
}
