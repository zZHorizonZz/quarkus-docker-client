package io.quarkiverse.docker.client.runtime;

import org.apache.commons.lang3.SystemUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

import io.quarkiverse.docker.client.runtime.config.DockerClientRuntimeConfig;

public class DockerClientFactory {

    private final DockerClientRuntimeConfig config;

    public DockerClientFactory(DockerClientRuntimeConfig config) {
        this.config = config;
    }

    public DockerClient client() {
        DockerClientConfig clientConfig = buildDockerClientConfig();
        DockerHttpClient httpClient = buildDockerHttpClient(clientConfig);
        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    private DockerClientConfig buildDockerClientConfig() {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        config.dockerHost().ifPresentOrElse(
                configBuilder::withDockerHost,
                () -> {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        configBuilder.withDockerHost("npipe:////./pipe/docker_engine");
                    } else if (SystemUtils.IS_OS_UNIX) {
                        configBuilder.withDockerHost("unix:///var/run/docker.sock");
                    } else {
                        throw new IllegalStateException("Unsupported operating system");
                    }
                });

        return configBuilder.build();
    }

    private DockerHttpClient buildDockerHttpClient(DockerClientConfig config) {
        return new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(this.config.connectTimeout())
                .responseTimeout(this.config.readTimeout())
                .build();
    }
}
