package com.spms.backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AssignmentControllerTest {

    /* TDD SPECIFICATION:
       Uncomment and implement when AssignmentController is created.
       
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdvisorAssignmentService advisorAssignmentService;

    @MockBean
    private JuryAssignmentService juryAssignmentService;

    @Test
    @DisplayName("POST /api/v1/committees/{id}/advisors - Assigns advisor")
    void assignAdvisor_Returns201() throws Exception {
        doNothing().when(advisorAssignmentService).assignAdvisor(1L, 200L);

        mockMvc.perform(post("/api/v1/committees/1/advisors")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\": 200}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/v1/committees/{id}/advisors/{advisorId} - Removes advisor")
    void removeAdvisor_Returns204() throws Exception {
        doNothing().when(advisorAssignmentService).removeAdvisor(1L, 200L);

        mockMvc.perform(delete("/api/v1/committees/1/advisors/200")
                .requestAttr("jwt_userId", 100L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/committees/{id}/juries - Assigns jury")
    void assignJury_Returns201() throws Exception {
        doNothing().when(juryAssignmentService).assignJury(1L, 300L, "INTERNAL");

        mockMvc.perform(post("/api/v1/committees/1/juries")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\": 300, \"type\": \"INTERNAL\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/v1/committees/{id}/juries/{juryId} - Removes jury")
    void removeJury_Returns204() throws Exception {
        doNothing().when(juryAssignmentService).removeJury(1L, 300L);

        mockMvc.perform(delete("/api/v1/committees/1/juries/300")
                .requestAttr("jwt_userId", 100L))
                .andExpect(status().isNoContent());
    }
    */
}
