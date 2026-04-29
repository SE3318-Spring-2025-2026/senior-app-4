package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.ProfessorRegisterRequest;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.repository.InMemoryUserRepository;
import com.spms.backend.service.PasswordHashingService;
import com.spms.backend.service.ProfessorRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfessorControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ProfessorController professorController = new ProfessorController(
                new ProfessorRegistrationService(new InMemoryUserRepository(), new PasswordHashingService())
        );

        mockMvc = MockMvcBuilders.standaloneSetup(professorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerProfessorReturns201WhenRequestIsValid() throws Exception {
        ProfessorRegisterRequest request = new ProfessorRegisterRequest(
                "Dr. Jane Doe",
                "jane.doe@university.edu",
                "SecurePass123!",
                "professor"
        );

        mockMvc.perform(post("/api/v1/professors/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Professor registered successfully."))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("professor"));
    }

    @Test
    void registerProfessorReturns400WhenEmailIsInvalid() throws Exception {
        ProfessorRegisterRequest request = new ProfessorRegisterRequest(
                "Dr. Jane Doe",
                "not-an-email",
                "SecurePass123!",
                "professor"
        );

        mockMvc.perform(post("/api/v1/professors/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("email must be a valid email address."));
    }

    @Test
    void registerProfessorReturns400WhenRoleIsInvalid() throws Exception {
        ProfessorRegisterRequest request = new ProfessorRegisterRequest(
                "Dr. Jane Doe",
                "jane.doe@university.edu",
                "SecurePass123!",
                "advisor"
        );

        mockMvc.perform(post("/api/v1/professors/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("role must be either professor or coordinator."));
    }

    @Test
    void registerProfessorReturns409WhenEmailAlreadyExists() throws Exception {
        ProfessorRegisterRequest firstRequest = new ProfessorRegisterRequest(
                "Dr. Jane Doe",
                "jane.doe@university.edu",
                "SecurePass123!",
                "professor"
        );

        ProfessorRegisterRequest secondRequest = new ProfessorRegisterRequest(
                "Dr. Jane Doe",
                "JANE.DOE@UNIVERSITY.EDU",
                "SecurePass123!",
                "coordinator"
        );

        mockMvc.perform(post("/api/v1/professors/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/professors/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A user already exists for the given email."));
    }
}
