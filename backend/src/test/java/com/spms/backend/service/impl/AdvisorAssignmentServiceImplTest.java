package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.model.AuditLog;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import com.spms.backend.model.User;
import com.spms.backend.repository.AbstractStubJpaRepository;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.CommitteeAdvisorRepository;
import com.spms.backend.repository.CommitteeRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.CommitteeNotificationService;
import com.spms.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdvisorAssignmentServiceImplTest {

    private StubGroupRepository groupRepository;
    private AuditLogRepository auditLogRepository;
    private NotificationService notificationService;
    private CommitteeRepository committeeRepository;
    private UserRepository userRepository;
    private CommitteeAdvisorRepository committeeAdvisorRepository;
    private CommitteeNotificationService committeeNotificationService;

    private AdvisorAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        groupRepository = new StubGroupRepository();
        auditLogRepository = new StubAuditLogRepository();
        notificationService = org.mockito.Mockito.mock(NotificationService.class);
        committeeRepository = org.mockito.Mockito.mock(CommitteeRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        committeeAdvisorRepository = org.mockito.Mockito.mock(CommitteeAdvisorRepository.class);
        committeeNotificationService = org.mockito.Mockito.mock(CommitteeNotificationService.class);
        service = new AdvisorAssignmentServiceImpl(groupRepository, auditLogRepository, notificationService,
                committeeRepository, userRepository, committeeAdvisorRepository, committeeNotificationService);
    }

    @Test
    void coordinator_seesAssignedTeamsOnly_byDefault() {
        Group withAdvisor = group(1L, "Team A", 10L, "Dr. Smith");
        Group withoutAdvisor = group(2L, "Team B", null, null);
        groupRepository.save(withAdvisor);
        groupRepository.save(withoutAdvisor);

        AdvisorAssignmentListResponse res = service.listAdvisorAssignments("coordinator", 99L, null, null);

        assertEquals("success", res.status());
        assertEquals(1, res.data().size());
        assertEquals(1L, res.data().get(0).teamId());
        assertEquals(10L, res.data().get(0).advisorId());
        assertNull(res.data().get(0).assignedAt());
        assertNull(res.data().get(0).assignmentType());
    }

    @Test
    void coordinator_hasAdvisorFalse_listsUnassigned() {
        Group withAdvisor = group(1L, "Team A", 10L, "Dr. Smith");
        Group withoutAdvisor = group(2L, "Team B", null, null);
        groupRepository.save(withAdvisor);
        groupRepository.save(withoutAdvisor);

        AdvisorAssignmentListResponse res =
                service.listAdvisorAssignments("COORDINATOR", 99L, null, false);

        assertEquals(1, res.data().size());
        assertEquals(2L, res.data().get(0).teamId());
        assertNull(res.data().get(0).advisorId());
    }

    @Test
    void professor_seesOnlyTheirGroups() {
        Group mine = group(1L, "Mine", 5L, "Me");
        Group other = group(2L, "Other", 99L, "Other Prof");
        groupRepository.save(mine);
        groupRepository.save(other);

        AdvisorAssignmentListResponse res = service.listAdvisorAssignments("professor", 5L, null, null);

        assertEquals(1, res.data().size());
        assertEquals(5L, res.data().get(0).advisorId());
    }

    @Test
    void student_forbidden() {
        assertThrows(
                ForbiddenException.class,
                () -> service.listAdvisorAssignments("student", 1L, null, null));
    }

    private static Group group(Long id, String name, Long advisorUserId, String advisorName) {
        Group g = new Group();
        g.setId(id);
        g.setGroupName(name);
        g.setStatus(GroupStatus.ADVISED);
        User leader = new User();
        leader.setFullName("Leader");
        g.setLeader(leader);
        if (advisorUserId != null) {
            User advisor = new User();
            advisor.setUserId(advisorUserId);
            advisor.setFullName(advisorName);
            g.setAdvisor(advisor);
        }
        return g;
    }

    static class StubGroupRepository
            extends AbstractStubJpaRepository<Group, Long>
            implements GroupRepository {

        private final Map<Long, Group> store = new LinkedHashMap<>();

        @Override
        public <S extends Group> S save(S entity) {
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Group> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Group> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void deleteById(Long id) {
            store.remove(id);
        }

        @Override
        public void delete(Group entity) {
            if (entity != null) {
                store.remove(entity.getId());
            }
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public List<Group> findByStatus(GroupStatus status) {
            return store.values().stream().filter(g -> g.getStatus() == status).toList();
        }

        @Override
        public List<Group> findByAdvisorIsNullAndStatusNot(GroupStatus status) {
            return store.values().stream()
                    .filter(g -> g.getAdvisor() == null && g.getStatus() != status)
                    .toList();
        }

        @Override
        public long countByAdvisor_UserId(Long advisorId) {
            return store.values().stream()
                    .filter(g -> g.getAdvisor() != null && advisorId.equals(g.getAdvisor().getUserId()))
                    .count();
        }

        @Override
        public Page<Group> findByAdvisorId(Long advisorId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Object[]> countGroupsByStatus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Group> findAllWithStudentGroupFirst(Long studentId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Group> findAllNonDisbandedWithAdvisorAndLeaderFetched(GroupStatus disbandedStatus) {
            return store.values().stream()
                    .filter(g -> g.getStatus() != disbandedStatus)
                    .sorted(Comparator.comparing(Group::getId))
                    .toList();
        }

        @Override
        public <S extends Group, R> R findBy(
                org.springframework.data.jpa.domain.Specification<Group> spec,
                java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Group> findAll(org.springframework.data.jpa.domain.Specification<Group> spec, Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public List<Group> findAll(org.springframework.data.jpa.domain.Specification<Group> spec) {
            return List.of();
        }

        @Override
        public List<Group> findAll(org.springframework.data.jpa.domain.Specification<Group> spec, org.springframework.data.domain.Sort sort) {
            return List.of();
        }

        @Override
        public Optional<Group> findOne(org.springframework.data.jpa.domain.Specification<Group> spec) {
            return Optional.empty();
        }

        @Override
        public long count(org.springframework.data.jpa.domain.Specification<Group> spec) {
            return 0;
        }

        @Override
        public boolean exists(org.springframework.data.jpa.domain.Specification<Group> spec) {
            return false;
        }

        @Override
        public long delete(org.springframework.data.jpa.domain.Specification<Group> spec) {
            return 0;
        }
    }

    static class StubAuditLogRepository
            extends AbstractStubJpaRepository<AuditLog, Long>
            implements AuditLogRepository {

        private final Map<Long, AuditLog> store = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public <S extends AuditLog> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(nextId++);
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<AuditLog> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<AuditLog> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void deleteById(Long id) {
            store.remove(id);
        }

        @Override
        public void delete(AuditLog entity) {
            if (entity != null) {
                store.remove(entity.getId());
            }
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public Page<AuditLog> findByGroupId(Long groupId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<AuditLog> findByActionType(com.spms.backend.model.ActionType actionType, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<AuditLog> findTopByGroupIdAndActionTypeOrderByCreatedAtDesc(
                Long groupId, com.spms.backend.model.ActionType actionType) {
            return Optional.empty();
        }
    }
}
