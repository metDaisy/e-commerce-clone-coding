package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends DomainRepository<UserCredential> {

  boolean existsByEmail(String email);

  Optional<UserCredential> findByEmail(String email);

  Optional<UserCredential> findByIdForUpdate(UUID id);
}
