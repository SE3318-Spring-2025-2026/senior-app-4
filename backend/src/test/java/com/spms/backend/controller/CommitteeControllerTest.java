package com.spms.backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class CommitteeControllerTest {

    /* TDD SPECIFICATION:
       Uncomment and implement when CommitteeController is created.
       
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommitteeService committeeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/committees - Creates a committee")
    void createCommittee_Returns201() throws Exception {
        Committee req = new Committee();
        req.setCommitteeName("Test Committee");
        
        when(committeeService.createCommittee(any(), anyLong())).thenReturn(req);

        mockMvc.perform(post("/api/v1/committees")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/v1/committees/{id} - Updates committee")
    void updateCommittee_Returns200() throws Exception {
        Committee req = new Committee();
        req.setCommitteeName("Updated Committee");

        when(committeeService.updateCommittee(eq(1L), any(), anyLong())).thenReturn(req);

        mockMvc.perform(put("/api/v1/committees/1")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/committees/{id} - Deletes committee")
    void deleteCommittee_Returns204() throws Exception {
        doNothing().when(committeeService).deleteCommittee(1L, 100L);

        mockMvc.perform(delete("/api/v1/committees/1")
                .requestAttr("jwt_userId", 100L))
                .andExpect(status().isNoContent());
    }
    */
}
