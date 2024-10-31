package io.quarkiverse.docker.client.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import io.quarkiverse.docker.client.runtime.NamedDockerClient;

@Path("/named-docker-client")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class DockerNamedClientResource {

    @Inject
    DockerClient defaultClient;

    @Inject
    @NamedDockerClient("client1")
    DockerClient client1;

    @Inject
    @NamedDockerClient("client2")
    DockerClient client2;

    @GET
    @Path("/{clientName}/containers")
    public List<Container> listContainers(@PathParam("clientName") String clientName) {
        return getClient(clientName).listContainersCmd()
                .withShowAll(true)
                .exec();
    }

    @POST
    @Path("/{clientName}/containers/{imageName}")
    public String createContainer(
            @PathParam("clientName") String clientName,
            @PathParam("imageName") String imageName) throws InterruptedException {
        DockerClient client = getClient(clientName);

        // Pull the image first
        client.pullImageCmd(imageName)
                .start()
                .awaitCompletion();

        // Create and start container
        String containerId = client.createContainerCmd(imageName)
                .withName("test-container-" + clientName + "-" + System.currentTimeMillis())
                .exec()
                .getId();

        client.startContainerCmd(containerId).exec();

        return containerId;
    }

    @POST
    @Path("/{clientName}/containers/{containerId}/stop")
    public Response stopContainer(
            @PathParam("clientName") String clientName,
            @PathParam("containerId") String containerId) {
        getClient(clientName).stopContainerCmd(containerId)
                .withTimeout(10)
                .exec();
        return Response.ok().build();
    }

    @DELETE
    @Path("/{clientName}/containers/{containerId}")
    public Response removeContainer(
            @PathParam("clientName") String clientName,
            @PathParam("containerId") String containerId) {
        getClient(clientName).removeContainerCmd(containerId)
                .withForce(true)
                .exec();
        return Response.noContent().build();
    }

    private DockerClient getClient(String clientName) {
        return switch (clientName) {
            case "default" -> defaultClient;
            case "client1" -> client1;
            case "client2" -> client2;
            default -> throw new WebApplicationException("Unknown client: " + clientName, 400);
        };
    }
}
