package com.spms.backend.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Test helper: stubs out all JpaRepository methods that in-memory test repos don't need.
 * Subclasses only override the methods their tests actually use.
 */
public abstract class AbstractStubJpaRepository<T, I> implements org.springframework.data.jpa.repository.JpaRepository<T, I> {

    @Override public void flush() {}

    @Override public <S extends T> S saveAndFlush(S entity) { return save(entity); }

    @Override public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override public void deleteAllInBatch(Iterable<T> entities) {
        entities.forEach(this::delete);
    }

    @Override public void deleteAllByIdInBatch(Iterable<I> ids) {
        ids.forEach(this::deleteById);
    }

    @Override public void deleteAllInBatch() { deleteAll(); }

    @Override public T getReferenceById(I id) {
        return findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(id.toString()));
    }

    @Override @Deprecated public T getById(I id) { return getReferenceById(id); }

    @Override @Deprecated public T getOne(I id) { return getReferenceById(id); }

    @Override public <S extends T> Optional<S> findOne(Example<S> example) { return Optional.empty(); }

    @Override public <S extends T> List<S> findAll(Example<S> example) { return Collections.emptyList(); }

    @Override public <S extends T> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }

    @Override public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        return org.springframework.data.support.PageableExecutionUtils.getPage(Collections.emptyList(), pageable, () -> 0);
    }

    @Override public <S extends T> long count(Example<S> example) { return 0; }

    @Override public <S extends T> boolean exists(Example<S> example) { return false; }

    @Override public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException();
    }

    @Override public List<T> findAll(Sort sort) { return findAll(); }

    @Override public Page<T> findAll(Pageable pageable) {
        return org.springframework.data.support.PageableExecutionUtils.getPage(findAll(), pageable, this::count);
    }

    @SuppressWarnings("unchecked")
    @Override public <S extends T> List<S> saveAll(Iterable<S> entities) {
        entities.forEach(this::save);
        return (List<S>) findAll();
    }

    @Override public boolean existsById(I id) { return findById(id).isPresent(); }

    @Override public List<T> findAllById(Iterable<I> ids) { return Collections.emptyList(); }

    @Override public void deleteAllById(Iterable<? extends I> ids) { ids.forEach(this::deleteById); }

    @Override public void deleteAll(Iterable<? extends T> entities) { entities.forEach(this::delete); }
}
