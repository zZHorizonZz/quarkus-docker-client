package io.quarkiverse.docker.client.runtime.config;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DockerClientRuntimeConfig {

    /**
     * Whether this client is enabled.
     * For the default client this is true by default, for named clients it's false by default.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Docker daemon connection timeout
     */
    @WithDefault("10s")
    Duration connectTimeout();

    /**
     * Docker API read timeout
     */
    @WithDefault("30s")
    Duration readTimeout();

    /**
     * Custom Docker host URL
     */
    Optional<String> dockerHost();
}
