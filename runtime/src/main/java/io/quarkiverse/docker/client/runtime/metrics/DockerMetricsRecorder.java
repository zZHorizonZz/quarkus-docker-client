package io.quarkiverse.docker.client.runtime.metrics;

import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;

@Recorder
public class DockerMetricsRecorder {

    public Consumer<MetricsFactory> registerMetrics() {
        return metricsFactory -> {
            DockerClientMetrics metrics = Arc.container().instance(DockerClientMetrics.class).get();

            metricsFactory.builder("docker.clients.active")
                    .description("Number of active Docker clients")
                    .buildGauge(metrics::getActiveClientCount);

            metricsFactory.builder("docker.clients.configured")
                    .description("Number of configured Docker clients")
                    .buildGauge(metrics::getConfiguredClientCount);

            // Add client-specific tags for each timeout metric
            metrics.getClientTimeouts().forEach((clientKey, duration) -> {
                String[] parts = clientKey.split("\\.");
                String clientName = parts[0];
                String timeoutType = parts[1];

                metricsFactory.builder("docker.client.timeout")
                        .description("Docker client timeout settings")
                        .tag("client", clientName)
                        .tag("type", timeoutType)
                        .buildGauge(() -> duration.toMillis());
            });

            // Add host URL metrics
            metrics.getClientHostUrls().forEach((clientName, url) -> {
                metricsFactory.builder("docker.client.info")
                        .description("Docker client configuration information")
                        .tag("client", clientName)
                        .tag("host", url)
                        .buildGauge(() -> 1);
            });
        };
    }
}
