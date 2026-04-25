package com.spms.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.backend.exception.GlobalExceptionHandler;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionStatus;
import com.spms.backend.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubmissionControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private SubmissionService submissionService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                submissionService = Mockito.mock(SubmissionService.class);

                SubmissionController controller = new SubmissionController(submissionService);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void testCreateRevision_Success() throws Exception {
                Long submissionId = 1L;
                Submission mockRevision = new Submission();
                mockRevision.setId(2L);
                mockRevision.setParentSubmissionId(submissionId);
                mockRevision.setVersion(2);
                mockRevision.setStatus(SubmissionStatus.PENDING);
                
                // Set the required fields
                mockRevision.setGroupId(10L);
                mockRevision.setDeliverableType("REPORT");
                mockRevision.setContent("Revision content");

                when(submissionService.createRevision(submissionId)).thenReturn(mockRevision);

                mockMvc.perform(post("/api/v1/submissions/{submissionId}/revisions", submissionId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Revision successfully created"))
                                .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        void testGetRevisions_Success() throws Exception {
                Long submissionId = 1L;
                Submission mockRevision = new Submission();
                mockRevision.setId(2L);
                mockRevision.setParentSubmissionId(submissionId);
                mockRevision.setVersion(2);
                
                // Set the required fields
                mockRevision.setGroupId(10L);
                mockRevision.setDeliverableType("REPORT");
                mockRevision.setContent("Revision content");

                when(submissionService.getRevisions(submissionId)).thenReturn(java.util.Collections.singletonList(mockRevision));

                mockMvc.perform(get("/api/v1/submissions/{submissionId}/revisions", submissionId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[0].version").value(2));
        }
}
