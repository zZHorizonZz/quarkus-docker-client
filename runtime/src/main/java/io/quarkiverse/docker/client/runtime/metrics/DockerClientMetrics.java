package io.quarkiverse.docker.client.runtime.metrics;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.docker.client.runtime.config.DockerClientRuntimeConfig;
import io.quarkiverse.docker.client.runtime.config.DockerRuntimeConfig;

@ApplicationScoped
public class DockerClientMetrics {

    private final DockerRuntimeConfig runtimeConfig;

    @Inject
    public DockerClientMetrics(DockerRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public long getActiveClientCount() {
        return 0;
    }

    public long getConfiguredClientCount() {
        return runtimeConfig.namedDockerClients().values()
                .stream()
                .filter(DockerClientRuntimeConfig::enabled)
                .count();
    }

    public Map<String, String> getClientHostUrls() {
        Map<String, String> urls = new HashMap<>();

        runtimeConfig.namedDockerClients().forEach((name, config) -> {
            if (config.enabled()) {
                urls.put(name, getHostUrl(config));
            }
        });

        return urls;
    }

    private String getHostUrl(DockerClientRuntimeConfig config) {
        return config.dockerHost().orElseGet(
                () -> System.getProperty("os.name").toLowerCase().contains("windows") ? "npipe:////./pipe/docker_engine"
                        : "unix:///var/run/docker.sock");
    }

    public Map<String, Duration> getClientTimeouts() {
        Map<String, Duration> timeouts = new HashMap<>();

        runtimeConfig.namedDockerClients().forEach((name, config) -> {
            if (config.enabled()) {
                timeouts.put(name + ".connect", config.connectTimeout());
                timeouts.put(name + ".read", config.readTimeout());
            }
        });

        return timeouts;
    }
}
