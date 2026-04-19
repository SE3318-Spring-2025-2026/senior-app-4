package com.spms.backend.service;

import com.spms.backend.dto.request.GradingCriteriaCreateRequestDto;
import com.spms.backend.dto.response.GradingCriteriaDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.User;
import com.spms.backend.repository.InMemoryGradingCriteriaRepository;
import com.spms.backend.repository.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradingCriteriaServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryGradingCriteriaRepository criteriaRepository;
    private GradingCriteriaService service;

    private User coordinator;
    private User professor;
    private User student;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        criteriaRepository = new InMemoryGradingCriteriaRepository();
        service = new GradingCriteriaService(criteriaRepository, userRepository);

        coordinator = makeUser("coordinator");
        professor = makeUser("professor");
        student = makeUser("student");
    }

    private User makeUser(String role) {
        User u = new User();
        u.setFullName("Test " + role);
        u.setEmail(role + "@test.com");
        u.setRole(role);
        u.setCreatedAt(Instant.now());
        if ("student".equals(role)) {
            u.setStudentId("1234567890" + role.length());
        } else {
            u.setPasswordHash("pbkdf2$65536$salt$hash");
            u.setRequiresPasswordChange(false);
        }
        return userRepository.save(u);
    }

    // ── create ──────────────────────────────────────────────────────────

    @Test
    void createAsCoordinator_succeeds() {
        var req = new GradingCriteriaCreateRequestDto("PROPOSAL", "Technical Feasibility", "Desc", 30.0);
        GradingCriteriaDto dto = service.create(req, coordinator.getUserId());

        assertNotNull(dto.id());
        assertEquals("PROPOSAL", dto.deliverableType());
        assertEquals("Technical Feasibility", dto.name());
        assertEquals("Desc", dto.description());
        assertEquals(30.0, dto.weight());
    }

    @Test
    void createAsProfessor_throwsForbidden() {
        var req = new GradingCriteriaCreateRequestDto("PROPOSAL", "Clarity", null, 20.0);
        assertThrows(ForbiddenException.class, () -> service.create(req, professor.getUserId()));
    }

    @Test
    void createAsStudent_throwsForbidden() {
        var req = new GradingCriteriaCreateRequestDto("PROPOSAL", "Clarity", null, 20.0);
        assertThrows(ForbiddenException.class, () -> service.create(req, student.getUserId()));
    }

    @Test
    void createWithInvalidDeliverableType_throwsBadRequest() {
        var req = new GradingCriteriaCreateRequestDto("INVALID_TYPE", "Name", null, 10.0);
        assertThrows(BadRequestException.class, () -> service.create(req, coordinator.getUserId()));
    }

    // ── list ─────────────────────────────────────────────────────────────

    @Test
    void listWithoutFilter_returnsAll() {
        service.create(new GradingCriteriaCreateRequestDto("PROPOSAL", "C1", null, 50.0), coordinator.getUserId());
        service.create(new GradingCriteriaCreateRequestDto("STATEMENT_OF_WORK", "C2", null, 50.0), coordinator.getUserId());

        List<GradingCriteriaDto> all = service.list(null);
        assertEquals(2, all.size());
    }

    @Test
    void listWithFilter_returnsOnlyMatchingType() {
        service.create(new GradingCriteriaCreateRequestDto("PROPOSAL", "C1", null, 50.0), coordinator.getUserId());
        service.create(new GradingCriteriaCreateRequestDto("STATEMENT_OF_WORK", "C2", null, 50.0), coordinator.getUserId());

        List<GradingCriteriaDto> proposals = service.list("PROPOSAL");
        assertEquals(1, proposals.size());
        assertEquals("PROPOSAL", proposals.get(0).deliverableType());
    }

    @Test
    void listWithBlankFilter_returnsAll() {
        service.create(new GradingCriteriaCreateRequestDto("PROPOSAL", "C1", null, 40.0), coordinator.getUserId());

        List<GradingCriteriaDto> all = service.list("  ");
        assertEquals(1, all.size());
    }

    @Test
    void listWithInvalidFilter_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.list("BOGUS"));
    }
}
