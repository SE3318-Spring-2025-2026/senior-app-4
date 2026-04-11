package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.StudentUserCreateRequest;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.repository.InMemoryUserRepository;
import com.spms.backend.repository.InMemoryValidStudentIdRepository;
import com.spms.backend.service.StudentRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        UserController userController = new UserController(
                new StudentRegistrationService(
                        userRepository,
                        new InMemoryValidStudentIdRepository()),
                userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerStudentReturns201WhenRequestIsValid() throws Exception {
        StudentUserCreateRequest request = new StudentUserCreateRequest(
                "11070001000",
                "furkangncr",
                "gho_valid"
        );

        mockMvc.perform(post("/api/v1/users/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User created successfully."))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.studentId").value("11070001000"))
                .andExpect(jsonPath("$.githubUsername").value("furkangncr"))
                .andExpect(jsonPath("$.role").value("student"));
    }

    @Test
    void registerStudentReturns400WhenBodyIsInvalid() throws Exception {
        String invalidRequestBody = """
                {
                  "studentId": "",
                  "githubUsername": "furkangncr",
                  "accessToken": "gho_valid"
                }
                """;

        mockMvc.perform(post("/api/v1/users/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("studentId is required."));
    }

    @Test
    void registerStudentReturns409WhenUserAlreadyExists() throws Exception {
        StudentUserCreateRequest request = new StudentUserCreateRequest(
                "11070001000",
                "furkangncr",
                "gho_valid"
        );

        mockMvc.perform(post("/api/v1/users/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A student user already exists for the given studentId."));
    }
}
