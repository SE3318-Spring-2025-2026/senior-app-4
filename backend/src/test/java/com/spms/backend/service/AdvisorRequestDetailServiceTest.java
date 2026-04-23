package com.spms.backend.service;

import com.spms.backend.dto.response.AdvisorRequestDetailDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.*;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.service.impl.AdvisorRequestDetailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdvisorRequestDetailServiceTest {

    private StubNotificationRepository notificationRepository;
    private StubGroupRepository groupRepository;
    private AdvisorRequestDetailService service;

    // ── Test fixtures ──────────────────────────────────────────────

    private User professor;
    private User leader;
    private User stranger;
    private User coordinator;
    private Group group;
    private Notification pendingRequest;

    @BeforeEach
    void setUp() {
        notificationRepository = new StubNotificationRepository();
        groupRepository = new StubGroupRepository();
        service = new AdvisorRequestDetailServiceImpl(notificationRepository, groupRepository);

        professor   = userWith(10L, "Dr. Smith",    "smith@uni.edu", "professor");
        leader      = userWith(20L, "Alice Leader",  "alice@stu.edu", "student");
        stranger    = userWith(30L, "Bob Stranger",  "bob@stu.edu",   "student");
        coordinator = userWith(40L, "Carol Coord",   "carol@uni.edu", "coordinator");

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

        pendingRequest = new Notification();
        pendingRequest.setId(1L);
        pendingRequest.setType(NotificationType.ADVISOR_REQUEST);
        pendingRequest.setStatus(NotificationStatus.PENDING);
        pendingRequest.setToUser(professor);
        pendingRequest.setFromUser(leader);
        pendingRequest.setGroupId(5L);
        pendingRequest.setMessage("Please be our advisor");
        pendingRequest.setCreatedAt(Instant.now());

        notificationRepository.save(pendingRequest);
        groupRepository.addGroup(group);
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

    @Test
    void statusMappingAccepted() {
        pendingRequest.setStatus(NotificationStatus.ACCEPTED);
        AdvisorRequestDetailDto dto = service.getDetail(1L, professor.getUserId(), "professor");
        assertEquals("APPROVED", dto.status());
    }

    @Test
    void statusMappingCleared() {
        pendingRequest.setStatus(NotificationStatus.CLEARED);
        AdvisorRequestDetailDto dto = service.getDetail(1L, professor.getUserId(), "professor");
        assertEquals("WITHDRAWN", dto.status());
    }

    // ── Error paths ────────────────────────────────────────────────

    @Test
    void unknownRequestIdThrows404() {
        assertThrows(NotFoundException.class,
                () -> service.getDetail(999L, professor.getUserId(), "professor"));
    }

    @Test
    void nullGroupIdThrows404() {
        pendingRequest.setGroupId(null);
        assertThrows(NotFoundException.class,
                () -> service.getDetail(1L, professor.getUserId(), "professor"));
    }

    @Test
    void nonAdvisorRequestNotificationThrows404() {
        Notification other = new Notification();
        other.setId(2L);
        other.setType(NotificationType.SYSTEM_ALERT);
        other.setStatus(NotificationStatus.PENDING);
        other.setToUser(professor);
        other.setGroupId(5L);
        notificationRepository.save(other);

        assertThrows(NotFoundException.class,
                () -> service.getDetail(2L, professor.getUserId(), "professor"));
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

    static class StubNotificationRepository
            extends com.spms.backend.repository.AbstractStubJpaRepository<Notification, Long>
            implements NotificationRepository {

        private final Map<Long, Notification> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override public <S extends Notification> S save(S entity) {
            if (entity.getId() == null) entity.setId(nextId++);
            store.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<Notification> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Notification> findAll() { return new ArrayList<>(store.values()); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(Notification e) { store.remove(e.getId()); }
        @Override public long count() { return store.size(); }
        @Override public void deleteAll() { store.clear(); }

        @Override public Page<Notification> findByToUser_UserId(Long toUserId, Pageable pageable) { return Page.empty(); }
        @Override public Optional<Notification> findByGroupIdAndTypeAndStatus(Long groupId, NotificationType type, com.spms.backend.model.notification.NotificationStatus status) { return Optional.empty(); }
        @Override public Optional<Notification> findByGroupIdAndToUser_UserIdAndTypeAndStatus(Long groupId, Long toUserId, NotificationType type, com.spms.backend.model.notification.NotificationStatus status) { return Optional.empty(); }
        @Override public void deleteByToUser_UserId(Long userId) {}
        @Override public List<Notification> findByToUser_UserIdAndTypeOrderByCreatedAtDesc(Long toUserId, NotificationType type) { return List.of(); }
        @Override public boolean existsByToUser_UserIdAndTypeAndStatusAndMessageContaining(Long toUserId, NotificationType type, com.spms.backend.model.notification.NotificationStatus status, String messageSnippet) { return false; }
        @Override public List<Notification> findByToUser_UserIdAndTypeAndStatus(Long toUserId, NotificationType type, com.spms.backend.model.notification.NotificationStatus status) { return List.of(); }
        @Override public List<Notification> findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(Long groupId, NotificationType type, com.spms.backend.model.notification.NotificationStatus status, Long excludedUserId) { return List.of(); }
    }

    static class StubGroupRepository
            extends com.spms.backend.repository.AbstractStubJpaRepository<Group, Long>
            implements GroupRepository {

        private final Map<Long, Group> store = new HashMap<>();
        private final Map<Long, Long> adviseeCountByProfessorId = new HashMap<>();

        void addGroup(Group group) { store.put(group.getId(), group); }

        void setAdviseeCount(Long professorId, long count) {
            adviseeCountByProfessorId.put(professorId, count);
        }

        @Override public <S extends Group> S save(S e) { store.put(e.getId(), e); return e; }
        @Override public Optional<Group> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Group> findAll() { return new ArrayList<>(store.values()); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(Group e) { store.remove(e.getId()); }
        @Override public long count() { return store.size(); }
        @Override public void deleteAll() { store.clear(); }

        @Override public List<Group> findByStatus(GroupStatus status) { return List.of(); }
        @Override public List<Group> findByAdvisorIsNullAndStatusNot(GroupStatus status) { return List.of(); }
        @Override public long countByAdvisor_UserId(Long advisorId) { return adviseeCountByProfessorId.getOrDefault(advisorId, 0L); }
        @Override public Page<Group> findByAdvisorId(Long advisorId, Pageable pageable) { return Page.empty(); }
        @Override public List<Object[]> countGroupsByStatus() { return List.of(); }
        @Override public Page<Group> findAllWithStudentGroupFirst(Long studentId, Pageable pageable) { return Page.empty(); }

        @Override
        public List<Group> findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus disbanded) {
            return findAll().stream()
                    .filter(g -> g.getStatus() != disbanded)
                    .toList();
        }
    }
}
