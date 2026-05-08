package com.spms.api;

import com.spms.backend.SpmsBackendApplication;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.ContentType;
import io.restassured.internal.http.HttpResponseException;
import org.apache.http.util.EntityUtils;
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

        RestAssured.requestSpecification = RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .log().ifValidationFails();

        // RestAssured 5.x registers the failure-response handler only for HttpPost inside
        // RestAssuredHttpBuilder.doRequest(), so GET/PUT requests that receive a 4xx/5xx
        // response end up in HTTPBuilder.defaultFailureHandler which throws
        // HttpResponseException instead of returning the response.  This filter intercepts
        // that exception and reconstructs a normal Response so that .then().statusCode(4xx)
        // assertions work correctly for all HTTP methods.
        RestAssured.replaceFiltersWith((requestSpec, responseSpec, ctx) -> {
            try {
                return ctx.next(requestSpec, responseSpec);
            } catch (Exception e) {
                // Groovy throws HttpResponseException (a checked IOException subclass) at runtime
                // through the filter chain without declaring it — catch Exception and check type.
                HttpResponseException hre = unwrapHttpResponseException(e);
                if (hre != null) {
                    String body = "";
                    try {
                        org.apache.http.HttpEntity entity = hre.getResponse().getEntity();
                        if (entity != null && entity.isRepeatable()) {
                            body = EntityUtils.toString(entity);
                        } else if (entity != null) {
                            body = EntityUtils.toString(entity);
                        }
                    } catch (Exception ignored) { /* entity already consumed or stream closed */ }
                    org.apache.http.Header ct = hre.getResponse().getFirstHeader("Content-Type");
                    String contentType = ct != null ? ct.getValue() : "application/json";
                    return new ResponseBuilder()
                            .setStatusCode(hre.getStatusCode())
                            .setBody(body)
                            .setContentType(contentType)
                            .build();
                }
                throw new RuntimeException(e);
            }
        });

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private static HttpResponseException unwrapHttpResponseException(Throwable t) {
        while (t != null) {
            if (t instanceof HttpResponseException) return (HttpResponseException) t;
            t = t.getCause();
        }
        return null;
    }
}
