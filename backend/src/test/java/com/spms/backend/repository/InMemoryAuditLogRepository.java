package com.spms.backend.repository;

import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
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

public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final Map<Long, AuditLog> logs = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends AuditLog> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        logs.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Page<AuditLog> findByGroupId(Long groupId, Pageable pageable) {
        List<AuditLog> matches = logs.values().stream()
                .filter(l -> l.getGroupId() != null && l.getGroupId().equals(groupId))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), matches.size());
        if (start > matches.size()) return new PageImpl<>(Collections.emptyList(), pageable, matches.size());
        return new PageImpl<>(matches.subList(start, end), pageable, matches.size());
    }

    @Override
    public Page<AuditLog> findByActionType(ActionType actionType, Pageable pageable) {
        List<AuditLog> matches = logs.values().stream()
                .filter(l -> l.getActionType() == actionType)
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), matches.size());
        if (start > matches.size()) return new PageImpl<>(Collections.emptyList(), pageable, matches.size());
        return new PageImpl<>(matches.subList(start, end), pageable, matches.size());
    }

    // --- Unimplemented Boilerplate ---

    @Override public List<AuditLog> findAll() { return new ArrayList<>(logs.values()); }
    @Override public List<AuditLog> findAll(Sort sort) { return findAll(); }
    @Override public Page<AuditLog> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public List<AuditLog> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return logs.size(); }
    @Override public void deleteById(Long id) { logs.remove(id); }
    @Override public void delete(AuditLog entity) { deleteById(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends AuditLog> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { logs.clear(); }
    @Override public boolean existsById(Long id) { return logs.containsKey(id); }
    @Override public Optional<AuditLog> findById(Long id) { return Optional.ofNullable(logs.get(id)); }
    @Override public <S extends AuditLog> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) entities; }
    @Override public void flush() {}
    @Override public <S extends AuditLog> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends AuditLog> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<AuditLog> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public AuditLog getOne(Long id) { return findById(id).orElse(null); }
    @Override public AuditLog getById(Long id) { return getOne(id); }
    @Override public AuditLog getReferenceById(Long id) { return getOne(id); }
    @Override public <S extends AuditLog> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends AuditLog> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends AuditLog> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends AuditLog> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends AuditLog> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends AuditLog> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends AuditLog, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
