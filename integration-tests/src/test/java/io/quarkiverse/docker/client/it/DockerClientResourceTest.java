package io.quarkiverse.docker.client.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DockerClientResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/docker-client")
                .then()
                .statusCode(200)
                .body(is("Hello docker-client"));
    }
}
