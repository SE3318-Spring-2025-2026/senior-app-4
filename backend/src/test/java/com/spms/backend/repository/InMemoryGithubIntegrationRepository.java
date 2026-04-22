package com.spms.backend.repository;

import com.spms.backend.model.GithubIntegration;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class InMemoryGithubIntegrationRepository implements GithubIntegrationRepository {

    private final Map<Long, GithubIntegration> integrations = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends GithubIntegration> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        integrations.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<GithubIntegration> findByGroup_Id(Long groupId) {
        return integrations.values().stream()
                .filter(i -> i.getGroup().getId().equals(groupId))
                .findFirst();
    }

    // --- Unimplemented Boilerplate ---

    @Override public List<GithubIntegration> findAll() { return new ArrayList<>(integrations.values()); }
    @Override public List<GithubIntegration> findAll(Sort sort) { return findAll(); }
    @Override public Page<GithubIntegration> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public List<GithubIntegration> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return integrations.size(); }
    @Override public void deleteById(Long id) { integrations.remove(id); }
    @Override public void delete(GithubIntegration entity) { deleteById(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends GithubIntegration> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { integrations.clear(); }
    @Override public boolean existsById(Long id) { return integrations.containsKey(id); }
    @Override public Optional<GithubIntegration> findById(Long id) { return Optional.ofNullable(integrations.get(id)); }
    @Override public <S extends GithubIntegration> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) entities; }
    @Override public void flush() {}
    @Override public <S extends GithubIntegration> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends GithubIntegration> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<GithubIntegration> entities) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
    @Override public GithubIntegration getOne(Long id) { return findById(id).orElse(null); }
    @Override public GithubIntegration getById(Long id) { return getOne(id); }
    @Override public GithubIntegration getReferenceById(Long id) { return getOne(id); }
    @Override public <S extends GithubIntegration> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GithubIntegration> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GithubIntegration> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends GithubIntegration> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends GithubIntegration> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GithubIntegration> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends GithubIntegration, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
