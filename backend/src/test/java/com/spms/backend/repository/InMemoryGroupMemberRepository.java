package com.spms.backend.repository;

import com.spms.backend.model.GroupMember;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryGroupMemberRepository implements GroupMemberRepository {

    private final Map<Long, GroupMember> members = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends GroupMember> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        members.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<GroupMember> findById(Long id) {
        return Optional.ofNullable(members.get(id));
    }

    @Override
    public boolean existsByUser_UserId(Long userId) {
        return members.values().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
    }

    @Override
    public boolean existsByGroup_IdAndUser_UserId(Long groupId, Long userId) {
        return members.values().stream()
                .anyMatch(m -> m.getGroup().getId().equals(groupId) && m.getUser().getUserId().equals(userId));
    }

    @Override
    public void deleteAll(Iterable<? extends GroupMember> entities) {
        entities.forEach(e -> members.remove(e.getId()));
    }

    // --- Unimplemented Boilerplate ---

    @Override public List<GroupMember> findAll() { return new ArrayList<>(members.values()); }
    @Override public List<GroupMember> findAll(Sort sort) { return findAll(); }
    @Override public Page<GroupMember> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public List<GroupMember> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return members.size(); }
    @Override public void deleteById(Long id) { members.remove(id); }
    @Override public void delete(GroupMember entity) { deleteById(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll() { members.clear(); }
    @Override public boolean existsById(Long id) { return members.containsKey(id); }
    @Override public <S extends GroupMember> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) entities; }
    @Override public void flush() {}
    @Override public <S extends GroupMember> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends GroupMember> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<GroupMember> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { deleteAllById(ids); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public GroupMember getOne(Long id) { return findById(id).orElse(null); }
    @Override public GroupMember getById(Long id) { return getOne(id); }
    @Override public GroupMember getReferenceById(Long id) { return getOne(id); }
    @Override public <S extends GroupMember> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GroupMember> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GroupMember> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends GroupMember> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends GroupMember> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GroupMember> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GroupMember, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
