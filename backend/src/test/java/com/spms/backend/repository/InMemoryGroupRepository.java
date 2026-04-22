package com.spms.backend.repository;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryGroupRepository implements GroupRepository {

    private final Map<Long, Group> groups = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends Group> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        groups.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Group> findById(Long id) {
        return Optional.ofNullable(groups.get(id));
    }

    @Override
    public List<Group> findAll() {
        return new ArrayList<>(groups.values());
    }

    @Override
    public void delete(Group entity) {
        groups.remove(entity.getId());
    }

    @Override
    public boolean existsById(Long id) {
        return groups.containsKey(id);
    }

    @Override
    public Page<Group> findAllWithStudentGroupFirst(Long studentId, Pageable pageable) {
        List<Group> all = new ArrayList<>(groups.values());
        all.sort((g1, g2) -> {
            boolean g1IsStudent = g1.getMembers().stream().anyMatch(m -> m.getUser().getUserId().equals(studentId));
            boolean g2IsStudent = g2.getMembers().stream().anyMatch(m -> m.getUser().getUserId().equals(studentId));
            if (g1IsStudent && !g2IsStudent) return -1;
            if (!g1IsStudent && g2IsStudent) return 1;
            return g1.getId().compareTo(g2.getId());
        });
        return toPage(all, pageable);
    }

    @Override
    public Page<Group> findByAdvisorId(Long advisorId, Pageable pageable) {
        List<Group> matches = groups.values().stream()
                .filter(g -> g.getAdvisor() != null && g.getAdvisor().getUserId().equals(advisorId))
                .collect(Collectors.toList());
        return toPage(matches, pageable);
    }

    @Override
    public List<Object[]> countGroupsByStatus() {
        Map<GroupStatus, Long> counts = groups.values().stream()
                .collect(Collectors.groupingBy(Group::getStatus, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> new Object[]{e.getKey(), e.getValue()})
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> findByStatus(GroupStatus status) {
        return groups.values().stream().filter(g -> g.getStatus() == status).collect(Collectors.toList());
    }

    @Override
    public List<Group> findByAdvisorIsNullAndStatusNot(GroupStatus status) {
        return groups.values().stream().filter(g -> g.getAdvisor() == null && g.getStatus() != status).collect(Collectors.toList());
    }

    private Page<Group> toPage(List<Group> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    @Override public List<Group> findAll(Sort sort) { return findAll(); }
    @Override public Page<Group> findAll(Pageable pageable) { return toPage(findAll(), pageable); }
    @Override public List<Group> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return groups.size(); }
    @Override public void deleteById(Long id) { groups.remove(id); }
    @Override public void deleteAll(Iterable<? extends Group> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll() { groups.clear(); }
    @Override public <S extends Group> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) entities; }
    @Override public void flush() {}
    @Override public <S extends Group> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Group> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Group> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { deleteAllById(ids); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public Group getOne(Long id) { return findById(id).orElse(null); }
    @Override public Group getById(Long id) { return getOne(id); }
    @Override public Group getReferenceById(Long id) { return getOne(id); }
    @Override public <S extends Group> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Group> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Group> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends Group> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends Group> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Group> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Group, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
