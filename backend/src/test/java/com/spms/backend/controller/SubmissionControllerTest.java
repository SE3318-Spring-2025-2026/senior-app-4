package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.dto.request.GradeUpdateRequest;
import com.spms.backend.dto.response.ScheduleResponse;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.model.Grade;
import com.spms.backend.repository.GradeRepository;
import com.spms.backend.service.GradeService;
import com.spms.backend.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubmissionControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private GradeRepository gradeRepository;
        private ScheduleService scheduleService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();

                gradeRepository = Mockito.mock(GradeRepository.class);
                scheduleService = Mockito.mock(ScheduleService.class);

                GradeService gradeService = new GradeService(gradeRepository, scheduleService);
                SubmissionController controller = new SubmissionController(gradeService);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .setValidator(validator)
                                .build();
        }

        @Test
        void testUpdateGrade_Success() throws Exception {
                Long submissionId = 1L;
                Long gradeId = 1L;
                Long professorId = 100L;

                Grade mockGrade = new Grade(gradeId, submissionId, professorId, 85, "Good");
                when(gradeRepository.findByIdAndSubmissionId(gradeId, submissionId)).thenReturn(Optional.of(mockGrade));

                // Mock schedule with a future deadline
                Instant futureDate = Instant.now().plus(5, ChronoUnit.DAYS);
                when(scheduleService.getLatestSchedule()).thenReturn(
                                new ScheduleResponse(1L, futureDate.toString(), futureDate.toString(),
                                                Instant.now().toString()));

                GradeUpdateRequest req = new GradeUpdateRequest(95, "Excellent");

                mockMvc.perform(put("/api/v1/submissions/{submissionId}/grades/{gradeId}", submissionId, gradeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .requestAttr("jwt_userId", professorId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                Mockito.verify(gradeRepository).save(any(Grade.class));
        }

        @Test
        void testUpdateGrade_ForbiddenNotOwnGrade() throws Exception {
                Long submissionId = 1L;
                Long gradeId = 1L;
                Long realProfessorId = 100L;
                Long requestingProfessorId = 200L;

                Grade mockGrade = new Grade(gradeId, submissionId, realProfessorId, 85, "Good");
                when(gradeRepository.findByIdAndSubmissionId(gradeId, submissionId)).thenReturn(Optional.of(mockGrade));

                GradeUpdateRequest req = new GradeUpdateRequest(95, "Excellent");

                mockMvc.perform(put("/api/v1/submissions/{submissionId}/grades/{gradeId}", submissionId, gradeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .requestAttr("jwt_userId", requestingProfessorId))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("You can only update your own grade record."));
        }

        @Test
        void testUpdateGrade_ForbiddenDeadlinePassed() throws Exception {
                Long submissionId = 1L;
                Long gradeId = 1L;
                Long professorId = 100L;

                Grade mockGrade = new Grade(gradeId, submissionId, professorId, 85, "Good");
                when(gradeRepository.findByIdAndSubmissionId(gradeId, submissionId)).thenReturn(Optional.of(mockGrade));

                // Mock schedule with a past deadline
                Instant pastDate = Instant.now().minus(5, ChronoUnit.DAYS);
                when(scheduleService.getLatestSchedule()).thenReturn(
                                new ScheduleResponse(1L, pastDate.toString(), pastDate.toString(),
                                                Instant.now().toString()));

                GradeUpdateRequest req = new GradeUpdateRequest(95, "Excellent");

                mockMvc.perform(put("/api/v1/submissions/{submissionId}/grades/{gradeId}", submissionId, gradeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .requestAttr("jwt_userId", professorId))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Grading deadline has passed."));
        }
}
