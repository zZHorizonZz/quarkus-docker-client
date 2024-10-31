package io.quarkiverse.docker.client.runtime;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Qualifier annotation used to identify and inject named Docker clients.
 * This annotation allows the application to work with multiple Docker client instances,
 * each configured differently and identified by a unique name.
 *
 * <p>
 * Usage examples:
 * </p>
 *
 * <pre>
 * // Inject a specific named Docker client
 * {@literal @}Inject
 * {@literal @}NamedDockerClient("production")
 * DockerClient productionClient;
 *
 * // Inject the default Docker client (empty value)
 * {@literal @}Inject
 * {@literal @}NamedDockerClient
 * DockerClient defaultClient;
 *
 * // Method parameter injection
 * public void processContainer(
 *     {@literal @}NamedDockerClient("staging") DockerClient client,
 *     String containerId
 * ) {
 *     // Use the staging client
 * }
 * </pre>
 *
 * <p>
 * Configuration example in application.properties:
 * </p>
 *
 * <pre>
 * quarkus.docker.production.docker-host=tcp://prod-host:2375
 * quarkus.docker.staging.docker-host=tcp://stage-host:2375
 * </pre>
 *
 * @see io.quarkiverse.docker.client.runtime.config.DockerClientRuntimeConfig
 * @see io.quarkiverse.docker.client.runtime.config.DockerRuntimeConfig
 *
 * @since 1.0
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
public @interface NamedDockerClient {

    /**
     * The name of the Docker client configuration to use.
     *
     * <p>
     * This name corresponds to the configuration prefix in application.properties.
     * For example, a value of "production" would use configuration properties
     * prefixed with "quarkus.docker.production".
     * </p>
     *
     * <p>
     * An empty string value (the default) refers to the default Docker client
     * configuration using the "quarkus.docker" prefix.
     * </p>
     *
     * @return The name of the Docker client configuration to use
     */
    String value() default "";
}
