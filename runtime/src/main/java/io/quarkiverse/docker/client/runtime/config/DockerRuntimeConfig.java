package io.quarkiverse.docker.client.runtime.config;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.docker")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface DockerRuntimeConfig {

    public static final String DOCKER_CONFIG_ROOT_NAME = "docker";
    public static final String DEFAULT_CLIENT_NAME = "<default>";

    /**
     * Docker default client configuration
     */
    @WithParentName
    DockerClientRuntimeConfig defaultDockerClient();

    /**
     * Whether to enable the Docker client health check
     */
    @WithName("health-check")
    Optional<Boolean> enableHealthCheck();

    /**
     * Docker clients configuration
     */
    @WithParentName
    @ConfigDocMapKey("docker-client-name")
    Map<String, DockerClientRuntimeConfig> namedDockerClients();

    static boolean isDefaultClient(String name) {
        return DEFAULT_CLIENT_NAME.equalsIgnoreCase(name);
    }

    static String propertyKey(String name, String radical) {
        String prefix = DEFAULT_CLIENT_NAME.equals(name)
                ? "quarkus.docker."
                : "quarkus.docker.\"" + name + "\".";

        return prefix + radical;
    }
}
