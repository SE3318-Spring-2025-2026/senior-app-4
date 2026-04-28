package com.spms.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test") // Uses H2 in-memory DB
public class CommitteeWorkflowIntegrationTest {

    /* TDD SPECIFICATION:
       Uncomment and implement when Process 5 endpoints and controllers are created.
       
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CommitteeRepository committeeRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        committeeRepository.deleteAll();
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Scenario 1 & Performance: Create committee -> Assign advisor -> Assign group -> Validate (< 2 seconds)")
    void scenario1_CompleteAssignmentWorkflow() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            // 1. Create Committee (POST /api/v1/committees)
            String committeeJson = "{\"name\": \"Senior Project Comm\", \"status\": \"ACTIVE\"}";
            MvcResult result = mockMvc.perform(post("/api/v1/committees")
                    .requestAttr("jwt_userId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(committeeJson))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            Long committeeId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

            // 2. Assign Advisor (POST /api/v1/committees/{id}/advisors)
            mockMvc.perform(post("/api/v1/committees/" + committeeId + "/advisors")
                    .requestAttr("jwt_userId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"professorId\": 200}"))
                    .andExpect(status().isCreated());

            // 3. Assign Group (POST /api/v1/committees/{id}/groups)
            mockMvc.perform(post("/api/v1/committees/" + committeeId + "/groups")
                    .requestAttr("jwt_userId", 100L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"groupId\": 50}"))
                    .andExpect(status().isCreated());

            // 4. Validate through GET endpoint
            mockMvc.perform(get("/api/v1/committees/" + committeeId)
                    .requestAttr("jwt_userId", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.advisors[0].id").value(200))
                    .andExpect(jsonPath("$.groups[0].id").value(50));
                    
            // 5. Data Consistency Verification (D5, D9, D10)
            // Verify Group (D5) was updated
            Group updatedGroup = groupRepository.findById(50L).orElseThrow();
            assertEquals(committeeId, updatedGroup.getCommitteeId(), "Group D5 was not updated with committeeId");
            
            // Verify Notification (D10) was sent to group members
            List<Notification> notifications = notificationRepository.findAll();
            boolean hasGroupNotification = notifications.stream()
                .anyMatch(n -> n.getType() == NotificationType.COMMITTEE_ASSIGNED);
            assertTrue(hasGroupNotification, "Notification D10 was not sent for group assignment");
            
            // Verify Audit Log (D9)
            List<AuditLog> logs = auditLogRepository.findAll();
            assertFalse(logs.isEmpty(), "Audit logs D9 should be generated for assignments");
            
        }, "Workflow execution exceeded 2 seconds limit!");
    }

    @Test
    @DisplayName("Scenario 2: Create committee -> Assign jury -> Delete jury -> Verify audit log")
    void scenario2_JuryLifecycleAndAudit() throws Exception {
        // 1. Create Committee
        MvcResult result = mockMvc.perform(post("/api/v1/committees")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Jury Comm\", \"status\": \"ACTIVE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long committeeId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        // 2. Assign Jury
        mockMvc.perform(post("/api/v1/committees/" + committeeId + "/juries")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\": 300, \"type\": \"EXTERNAL\"}"))
                .andExpect(status().isCreated());

        // 3. Delete Jury
        mockMvc.perform(delete("/api/v1/committees/" + committeeId + "/juries/300")
                .requestAttr("jwt_userId", 100L))
                .andExpect(status().isNoContent());

        // 4. Verify Audit Log
        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasDeleteLog = logs.stream()
                .anyMatch(log -> log.getActionType() == ActionType.JURY_REMOVED && log.getUserId() == 100L);
        assertTrue(hasDeleteLog, "Audit log for Jury Removal was not found!");
    }

    @Test
    @DisplayName("Scenario 3: Create committee -> Assign group with schedule conflict -> Verify error")
    void scenario3_ScheduleConflict_Returns400() throws Exception {
        // 1. Create Committee
        MvcResult result = mockMvc.perform(post("/api/v1/committees")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Conflict Comm\", \"status\": \"ACTIVE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long committeeId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        // 2. Assign Group (Simulating a schedule conflict via mock or specific group ID that conflicts)
        mockMvc.perform(post("/api/v1/committees/" + committeeId + "/groups")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\": 999}")) // Assuming group 999 is pre-configured to have a conflict
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Schedule conflict")));
    }

    @Test
    @DisplayName("Scenario 4: Get committee -> Filter by status -> Validate Pagination -> Update -> Delete")
    void scenario4_FilterUpdateDeleteLifecycle() throws Exception {
        // 1. Setup pre-populated data
        mockMvc.perform(post("/api/v1/committees")
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"List Comm\", \"status\": \"FORMING\"}"))
                .andExpect(status().isCreated());

        // 2. Filter by status (Testing GET endpoints with params)
        MvcResult getResult = mockMvc.perform(get("/api/v1/committees")
                .param("status", "FORMING")
                .param("page", "0")
                .param("size", "10")
                .requestAttr("jwt_userId", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();
                
        // Extract ID for further operations to reach the 30 endpoint coverage
        Long committeeId = JsonPath.read(getResult.getResponse().getContentAsString(), "$.content[0].id");
        
        // 3. Update Committee (PUT)
        mockMvc.perform(put("/api/v1/committees/" + committeeId)
                .requestAttr("jwt_userId", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Updated Comm\", \"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk());
                
        // 4. Delete Committee (DELETE)
        mockMvc.perform(delete("/api/v1/committees/" + committeeId)
                .requestAttr("jwt_userId", 100L))
                .andExpect(status().isNoContent());
                
        // 5. Verify cascade deletion in Audit Logs (D9)
        boolean hasDeleteLog = auditLogRepository.findAll().stream()
                .anyMatch(log -> log.getActionType() == ActionType.COMMITTEE_DELETED);
        assertTrue(hasDeleteLog, "Committee deletion audit log missing");
    }
    */
}
