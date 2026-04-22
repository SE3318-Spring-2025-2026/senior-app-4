package com.spms.backend.repository;

import com.spms.backend.model.JiraIntegration;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class InMemoryJiraIntegrationRepository implements JiraIntegrationRepository {

    private final Map<Long, JiraIntegration> integrations = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends JiraIntegration> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        integrations.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<JiraIntegration> findByGroup_Id(Long groupId) {
        return integrations.values().stream()
                .filter(i -> i.getGroup().getId().equals(groupId))
                .findFirst();
    }

    @Override
    public void deleteByGroup_Id(Long groupId) {
        integrations.values().removeIf(i -> i.getGroup().getId().equals(groupId));
    }

    @Override
    public void delete(JiraIntegration entity) {
        integrations.remove(entity.getId());
    }

    // --- Unimplemented Boilerplate ---

    @Override public List<JiraIntegration> findAll() { return new ArrayList<>(integrations.values()); }
    @Override public List<JiraIntegration> findAll(Sort sort) { return findAll(); }
    @Override public Page<JiraIntegration> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public List<JiraIntegration> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return integrations.size(); }
    @Override public void deleteById(Long id) { integrations.remove(id); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends JiraIntegration> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { integrations.clear(); }
    @Override public boolean existsById(Long id) { return integrations.containsKey(id); }
    @Override public Optional<JiraIntegration> findById(Long id) { return Optional.ofNullable(integrations.get(id)); }
    @Override public <S extends JiraIntegration> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) entities; }
    @Override public void flush() {}
    @Override public <S extends JiraIntegration> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends JiraIntegration> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<JiraIntegration> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public JiraIntegration getOne(Long id) { return findById(id).orElse(null); }
    @Override public JiraIntegration getById(Long id) { return getOne(id); }
    @Override public JiraIntegration getReferenceById(Long id) { return getOne(id); }
    @Override public <S extends JiraIntegration> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends JiraIntegration> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends JiraIntegration> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends JiraIntegration> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends JiraIntegration> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends JiraIntegration> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends JiraIntegration, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
