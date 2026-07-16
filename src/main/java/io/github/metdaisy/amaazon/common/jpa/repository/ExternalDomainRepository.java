package io.github.metdaisy.amaazon.common.jpa.repository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalDomainRepository<T> extends DomainRepository<T> {

  Optional<T> get(UUID id);
}
