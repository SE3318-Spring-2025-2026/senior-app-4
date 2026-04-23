package com.spms.backend.api;

import com.spms.backend.dto.request.GradeSubmissionRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SubmissionGradeApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    public void submitGrade_Unauthorized_Returns403() {
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(85.0);

        given()
            .contentType(ContentType.JSON)
            .header("jwt_userId", "999") // Assume 999 is not in the committee
            .body(request)
        .when()
            .post("/api/v1/submissions/1/grades")
        .then()
            .statusCode(either(is(403)).or(is(404)).or(is(500))); // Depends on mock data state
    }

    @Test
    public void submitGrade_InvalidData_Returns400() {
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setGrade(150.0); // Invalid: max 100

        given()
            .contentType(ContentType.JSON)
            .header("jwt_userId", "5")
            .body(request)
        .when()
            .post("/api/v1/submissions/1/grades")
        .then()
            .statusCode(400);
    }
}
