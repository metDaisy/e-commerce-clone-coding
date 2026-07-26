package io.github.metdaisy.amaazon.common.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface
public interface DomainRepository<T> {

  T save(T entity);

  <S extends T> List<S> saveAll(Iterable<S> entities);

  void delete(T entity);

  Optional<T> findById(UUID id);
}
