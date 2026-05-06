package com.spms.api;

import com.spms.backend.SpmsBackendApplication;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all API integration tests.
 *
 * <ul>
 *   <li>Starts the Spring Boot application on a random port.</li>
 *   <li>Automatically configures RestAssured (baseURI, port, contentType).</li>
 *   <li>Enables request/response logging on validation failures.</li>
 * </ul>
 *
 * Usage: {@code class MyApiTest extends BaseApiTest { ... }}
 */
@SpringBootTest(
    classes = SpmsBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseApiTest {

    @LocalServerPort
    protected int port;

    @BeforeAll
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.defaultParser = io.restassured.parsing.Parser.JSON;

        // Send JSON on every request and log on validation failure
        RestAssured.requestSpecification = RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .log().ifValidationFails();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
