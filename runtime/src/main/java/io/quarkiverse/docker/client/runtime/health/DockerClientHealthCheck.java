package io.quarkiverse.docker.client.runtime.health;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.github.dockerjava.api.DockerClient;

import io.quarkiverse.docker.client.runtime.NamedDockerClient;
import io.quarkiverse.docker.client.runtime.config.DockerRuntimeConfig;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

@Readiness
@ApplicationScoped
public class DockerClientHealthCheck implements HealthCheck {

    private final Map<String, DockerClient> clients = new HashMap<>();

    private final DockerRuntimeConfig config;

    public DockerClientHealthCheck(DockerRuntimeConfig config) {
        this.config = config;
    }

    @PostConstruct
    protected void init() {
        for (InstanceHandle<DockerClient> handle : Arc.container().select(DockerClient.class, Any.Literal.INSTANCE).handles()) {
            String clientName = getClientName(handle.getBean());
            clients.putIfAbsent(clientName == null ? DockerRuntimeConfig.DEFAULT_CLIENT_NAME : clientName, handle.get());
        }
    }

    private String getClientName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof NamedDockerClient) {
                return ((NamedDockerClient) qualifier).value();
            }
        }
        return null;
    }

    private Duration getTimeout(String name) {
        if (DockerRuntimeConfig.isDefaultClient(name)) {
            return config.defaultDockerClient().connectTimeout();
        } else {
            return config.namedDockerClients().get(name).connectTimeout();
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Redis connection health check").up();
        for (Map.Entry<String, DockerClient> client : clients.entrySet()) {
            try {
                boolean isDefault = DockerRuntimeConfig.DEFAULT_CLIENT_NAME.equals(client.getKey());
                DockerClient redisClient = client.getValue();
                String redisClientName = isDefault ? "default" : client.getKey();
                Duration timeout = getTimeout(client.getKey());

                Uni.createFrom().item(() -> redisClient.pingCmd().exec()).await().atMost(timeout);
                builder.up().withData(redisClientName, "client [" + redisClientName + "]: OK");
            } catch (TimeoutException e) {
                return builder.down().withData("reason", "client [" + client.getKey() + "]: timeout").build();
            } catch (Exception e) {
                if (e.getMessage() == null) {
                    return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e).build();
                }
                return builder.down().withData("reason", "client [" + client.getKey() + "]: " + e.getMessage()).build();
            }
        }
        return builder.build();
    }
}
