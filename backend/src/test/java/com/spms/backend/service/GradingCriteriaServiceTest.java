package com.spms.backend.service;

import com.spms.backend.dto.request.GradingCriteriaCreateRequestDto;
import com.spms.backend.dto.response.GradingCriteriaDto;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.repository.InMemoryGradingCriteriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradingCriteriaServiceTest {

    private static final Long CREATOR_ID = 1L;

    private InMemoryGradingCriteriaRepository criteriaRepository;
    private GradingCriteriaService service;

    @BeforeEach
    void setUp() {
        criteriaRepository = new InMemoryGradingCriteriaRepository();
        service = new GradingCriteriaService(criteriaRepository);
    }

    // ── create ──────────────────────────────────────────────────────────

    @Test
    void create_succeeds() {
        var req = new GradingCriteriaCreateRequestDto("PROPOSAL", "Technical Feasibility", "Desc", 30.0, null);
        GradingCriteriaDto dto = service.create(req, CREATOR_ID);

        assertNotNull(dto.id());
        assertEquals("PROPOSAL", dto.deliverableType());
        assertEquals("Technical Feasibility", dto.name());
        assertEquals("Desc", dto.description());
        assertEquals(30.0, dto.weight());
    }

    @Test
    void createWithInvalidDeliverableType_throwsBadRequest() {
        var req = new GradingCriteriaCreateRequestDto("INVALID_TYPE", "Name", null, 10.0, null);
        assertThrows(BadRequestException.class, () -> service.create(req, CREATOR_ID));
    }

    // ── list ─────────────────────────────────────────────────────────────

    @Test
    void listWithoutFilter_returnsAll() {
        service.create(new GradingCriteriaCreateRequestDto("PROPOSAL", "C1", null, 50.0, null), CREATOR_ID);
        service.create(new GradingCriteriaCreateRequestDto("STATEMENT_OF_WORK", "C2", null, 50.0, null), CREATOR_ID);

        List<GradingCriteriaDto> all = service.list(null);
        assertEquals(2, all.size());
    }

    @Test
    void listWithFilter_returnsOnlyMatchingType() {
        service.create(new GradingCriteriaCreateRequestDto("PROPOSAL", "C1", null, 50.0, null), CREATOR_ID);
        service.create(new GradingCriteriaCreateRequestDto("STATEMENT_OF_WORK", "C2", null, 50.0, null), CREATOR_ID);

        List<GradingCriteriaDto> proposals = service.list("PROPOSAL");
        assertEquals(1, proposals.size());
        assertEquals("PROPOSAL", proposals.get(0).deliverableType());
    }

    @Test
    void listWithBlankFilter_returnsAll() {
        service.create(new GradingCriteriaCreateRequestDto("PROPOSAL", "C1", null, 40.0, null), CREATOR_ID);

        List<GradingCriteriaDto> all = service.list("  ");
        assertEquals(1, all.size());
    }

    @Test
    void listWithInvalidFilter_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.list("BOGUS"));
    }
}
