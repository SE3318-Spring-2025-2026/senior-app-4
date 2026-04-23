package com.spms.backend.repository;

import com.spms.backend.model.DeliverableType;
import com.spms.backend.model.GradingCriteria;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryGradingCriteriaRepository
        extends AbstractStubJpaRepository<GradingCriteria, Long>
        implements GradingCriteriaRepository {

    private final Map<Long, GradingCriteria> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1L);

    @Override
    public <S extends GradingCriteria> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idSequence.getAndIncrement());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<GradingCriteria> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<GradingCriteria> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<GradingCriteria> findByDeliverableType(DeliverableType deliverableType) {
        return store.values().stream()
                .filter(c -> c.getDeliverableType() == deliverableType)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public void delete(GradingCriteria entity) {
        if (entity.getId() != null) store.remove(entity.getId());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public <S extends GradingCriteria> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        entities.forEach(e -> result.add(save(e)));
        return result;
    }

    @Override
    public List<GradingCriteria> findAllById(Iterable<Long> ids) {
        List<GradingCriteria> result = new ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(result::add));
        return result;
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        ids.forEach(store::remove);
    }
}
