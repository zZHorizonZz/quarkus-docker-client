/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.docker.client.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;

@Path("/docker-client")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class DockerClientResource {

    @Inject
    DockerClient dockerClient;

    @GET
    @Path("/info")
    public Response getInfo() {
        Info info = dockerClient.infoCmd().exec();
        return Response.ok(info).build();
    }

    @GET
    @Path("/containers")
    public Response listContainers() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();
        return Response.ok(containers).build();
    }

    @POST
    @Path("/containers/{imageName}")
    public Response createContainer(@PathParam("imageName") String imageName) {
        String containerId = dockerClient.createContainerCmd(imageName)
                .exec()
                .getId();
        return Response.ok(containerId).build();
    }

    @DELETE
    @Path("/containers/{containerId}")
    public Response removeContainer(@PathParam("containerId") String containerId) {
        dockerClient.removeContainerCmd(containerId)
                .withForce(true)
                .exec();
        return Response.noContent().build();
    }
}
