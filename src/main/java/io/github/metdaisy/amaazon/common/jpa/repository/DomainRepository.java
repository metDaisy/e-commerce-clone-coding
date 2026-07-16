package io.github.metdaisy.amaazon.common.jpa.repository;

import java.util.List;
import java.util.Optional;

public interface DomainRepository<T> {

  T save(T entity);

  <S extends T> List<S> saveAll(Iterable<S> entities);

  void delete(T entity);

  Optional<T> findById(long id);
}
