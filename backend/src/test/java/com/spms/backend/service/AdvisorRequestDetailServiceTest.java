package com.spms.backend.service;

import com.spms.backend.dto.response.AdvisorRequestDetailDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.*;
import com.spms.backend.repository.AdvisorRequestRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.mocks.MockAdvisorRequestDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdvisorRequestDetailServiceTest {

    private StubAdvisorRequestRepository advisorRequestRepository;
    private StubGroupRepository groupRepository;
    private AdvisorRequestDetailService service;

    // ── Test fixtures ──────────────────────────────────────────────

    private User professor;
    private User leader;
    private User stranger;
    private User coordinator;
    private Group group;
    private AdvisorRequest pendingRequest;

    @BeforeEach
    void setUp() {
        advisorRequestRepository = new StubAdvisorRequestRepository();
        groupRepository = new StubGroupRepository();
        service = new MockAdvisorRequestDetailService(advisorRequestRepository, groupRepository);

        professor = userWith(10L, "Dr. Smith", "smith@uni.edu", "professor");
        leader    = userWith(20L, "Alice Leader", "alice@stu.edu", "student");
        stranger  = userWith(30L, "Bob Stranger", "bob@stu.edu", "student");
        coordinator = userWith(40L, "Carol Coord", "carol@uni.edu", "coordinator");

        group = new Group();
        group.setId(5L);
        group.setGroupName("Alpha Team");
        group.setLeader(leader);
        group.setAdvisor(null);

        GroupMember m = new GroupMember();
        m.setUser(leader);
        m.setGroup(group);
        m.setRole(GroupRole.LEADER);
        group.getMembers().add(m);

        pendingRequest = new AdvisorRequest();
        pendingRequest.setId(1L);
        pendingRequest.setGroup(group);
        pendingRequest.setProfessor(professor);
        pendingRequest.setRequester(leader);
        pendingRequest.setMessage("Please be our advisor");
        pendingRequest.setStatus(AdvisorRequestStatus.PENDING);
        pendingRequest.setRequestedAt(Instant.now());

        advisorRequestRepository.save(pendingRequest);
        groupRepository.setAdviseeCount(professor.getUserId(), 2L);
    }

    // ── Happy path ─────────────────────────────────────────────────

    @Test
    void professorCanViewTheirOwnRequest() {
        AdvisorRequestDetailDto dto = service.getDetail(1L, professor.getUserId(), "professor");

        assertEquals(1L, dto.id());
        assertEquals("PENDING", dto.status());
        assertEquals("Please be our advisor", dto.message());
        assertNull(dto.decidedAt());
        assertNull(dto.reason());
    }

    @Test
    void teamInfoIsCorrect() {
        AdvisorRequestDetailDto dto = service.getDetail(1L, professor.getUserId(), "professor");

        AdvisorRequestDetailDto.TeamDto team = dto.team();
        assertEquals(5L, team.id());
        assertEquals("Alpha Team", team.name());
        assertEquals(1, team.memberCount());
        assertEquals("Alice Leader", team.leaderName());
        assertEquals(20L, team.leaderId());
        assertFalse(team.hasCurrentAdvisor());
    }

    @Test
    void professorInfoIncludesAdviseeCount() {
        AdvisorRequestDetailDto dto = service.getDetail(1L, professor.getUserId(), "professor");

        AdvisorRequestDetailDto.ProfessorDto prof = dto.professor();
        assertEquals(10L, prof.id());
        assertEquals("Dr. Smith", prof.fullName());
        assertEquals("smith@uni.edu", prof.email());
        assertEquals(2L, prof.currentAdviseeCount());
    }

    @Test
    void coordinatorCanViewAnyRequest() {
        AdvisorRequestDetailDto dto = service.getDetail(1L, coordinator.getUserId(), "COORDINATOR");
        assertEquals(1L, dto.id());
    }

    @Test
    void coordinatorRoleIsCaseInsensitive() {
        AdvisorRequestDetailDto dto = service.getDetail(1L, coordinator.getUserId(), "coordinator");
        assertEquals(1L, dto.id());
    }

    @Test
    void hasCurrentAdvisorTrueWhenAdvisorSet() {
        group.setAdvisor(professor);
        AdvisorRequestDetailDto dto = service.getDetail(1L, professor.getUserId(), "professor");
        assertTrue(dto.team().hasCurrentAdvisor());
    }

    // ── Error paths ────────────────────────────────────────────────

    @Test
    void unknownRequestIdThrows404() {
        assertThrows(NotFoundException.class,
                () -> service.getDetail(999L, professor.getUserId(), "professor"));
    }

    @Test
    void unrelatedProfessorThrows403() {
        assertThrows(ForbiddenException.class,
                () -> service.getDetail(1L, stranger.getUserId(), "professor"));
    }

    @Test
    void studentThrows403() {
        assertThrows(ForbiddenException.class,
                () -> service.getDetail(1L, leader.getUserId(), "student"));
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static User userWith(Long id, String name, String email, String role) {
        User u = new User();
        u.setUserId(id);
        u.setFullName(name);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    // ── In-memory stubs ────────────────────────────────────────────

    static class StubAdvisorRequestRepository
            extends com.spms.backend.repository.AbstractStubJpaRepository<AdvisorRequest, Long>
            implements AdvisorRequestRepository {

        private final Map<Long, AdvisorRequest> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override public <S extends AdvisorRequest> S save(S entity) {
            if (entity.getId() == null) entity.setId(nextId++);
            store.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<AdvisorRequest> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<AdvisorRequest> findAll() { return new ArrayList<>(store.values()); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(AdvisorRequest e) { store.remove(e.getId()); }
        @Override public long count() { return store.size(); }
        @Override public void deleteAll() { store.clear(); }
    }

    static class StubGroupRepository
            extends com.spms.backend.repository.AbstractStubJpaRepository<Group, Long>
            implements GroupRepository {

        private final Map<Long, Long> adviseeCountByProfessorId = new HashMap<>();

        void setAdviseeCount(Long professorId, long count) {
            adviseeCountByProfessorId.put(professorId, count);
        }

        @Override public <S extends Group> S save(S e) { return e; }
        @Override public Optional<Group> findById(Long id) { return Optional.empty(); }
        @Override public List<Group> findAll() { return List.of(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(Group e) {}
        @Override public long count() { return 0; }
        @Override public void deleteAll() {}

        @Override
        public List<Group> findByStatus(GroupStatus status) { return List.of(); }

        @Override
        public List<Group> findByAdvisorIsNullAndStatusNot(GroupStatus status) { return List.of(); }

        @Override
        public long countByAdvisor_UserId(Long advisorId) {
            return adviseeCountByProfessorId.getOrDefault(advisorId, 0L);
        }
    }
}
