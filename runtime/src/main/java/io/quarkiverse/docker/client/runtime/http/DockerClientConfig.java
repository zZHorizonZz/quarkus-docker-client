package io.quarkiverse.docker.client.runtime.http;

import java.time.Duration;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * Configuration for Docker HTTP client.
 */
public class DockerClientConfig {
    private final String dockerHost;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final boolean tlsVerify;
    private final String certPath;

    @RecordableConstructor
    public DockerClientConfig(
            String dockerHost,
            Duration connectTimeout,
            Duration readTimeout,
            boolean tlsVerify,
            String certPath) {
        this.dockerHost = dockerHost;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.tlsVerify = tlsVerify;
        this.certPath = certPath;
    }

    public String dockerHost() {
        return dockerHost;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout;
    }

    public boolean tlsVerify() {
        return tlsVerify;
    }

    public String certPath() {
        return certPath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dockerHost = defaultDockerHost();
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private boolean tlsVerify = false;
        private String certPath;

        private static String defaultDockerHost() {
            return System.getProperty("os.name").toLowerCase().contains("windows") ? "npipe:////./pipe/docker_engine"
                    : "unix:///var/run/docker.sock";
        }

        public Builder dockerHost(String dockerHost) {
            this.dockerHost = dockerHost;
            return this;
        }

        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public Builder tlsVerify(boolean verify) {
            this.tlsVerify = verify;
            return this;
        }

        public Builder certPath(String path) {
            this.certPath = path;
            return this;
        }

        public DockerClientConfig build() {
            return new DockerClientConfig(
                    dockerHost,
                    connectTimeout,
                    readTimeout,
                    tlsVerify,
                    certPath);
        }
    }
}
