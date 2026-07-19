package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;

public interface UserCredentialRepository extends DomainRepository<UserCredential> {

  boolean existsByEmail(String email);
}
