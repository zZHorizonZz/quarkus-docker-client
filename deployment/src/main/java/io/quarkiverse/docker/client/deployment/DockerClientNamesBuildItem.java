package io.quarkiverse.docker.client.deployment;

import java.util.Set;

import io.quarkiverse.docker.client.runtime.NamedDockerClient;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that holds the names of all Docker clients configured in the application. This includes both explicitly named
 * clients through {@link NamedDockerClient} annotation
 * and the default client.
 */
public final class DockerClientNamesBuildItem extends SimpleBuildItem {

    private final Set<String> dockerClientNames;

    public DockerClientNamesBuildItem(Set<String> dockerClientNames) {
        if (dockerClientNames == null) {
            throw new NullPointerException("dockerClientNames cannot be null");
        }
        this.dockerClientNames = Set.copyOf(dockerClientNames); // Create immutable copy
    }

    /**
     * Returns the set of Docker client names.
     *
     * @return An unmodifiable Set containing the Docker client names
     */
    public Set<String> getDockerClientNames() {
        return dockerClientNames;
    }
}
