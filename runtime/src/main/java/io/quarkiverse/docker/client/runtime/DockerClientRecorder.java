package io.quarkiverse.docker.client.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.github.dockerjava.api.DockerClient;

import io.quarkiverse.docker.client.runtime.config.DockerClientRuntimeConfig;
import io.quarkiverse.docker.client.runtime.config.DockerRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DockerClientRecorder {

    private final DockerRuntimeConfig config;
    private static final Map<String, DockerClient> clients = new HashMap<>();

    public DockerClientRecorder(DockerRuntimeConfig config) {
        this.config = config;
    }

    public void initialize(Set<String> names) {
        for (String name : names) {
            Optional<DockerClientRuntimeConfig> clientConfig = getConfigForName(config, name);
            if (clientConfig.isEmpty()) {
                throw new IllegalStateException("No configuration found for Docker client: " + name);
            }

            clients.computeIfAbsent(name, k -> new DockerClientFactory(clientConfig.get()).client());
        }
    }

    static Optional<DockerClientRuntimeConfig> getConfigForName(DockerRuntimeConfig cfg, String name) {
        if (DockerRuntimeConfig.isDefaultClient(name)) {
            return Optional.ofNullable(cfg.defaultDockerClient());
        }

        for (Map.Entry<String, DockerClientRuntimeConfig> entry : cfg.namedDockerClients().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public Supplier<DockerClient> createDockerClientBean() {
        return () -> clients.get(DockerRuntimeConfig.DEFAULT_CLIENT_NAME);
    }

    public Supplier<DockerClient> createNamedDockerClientBean(String clientName) {
        return () -> clients.get(clientName);
    }
}
