package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;
import java.util.UUID;

public interface BlacklistUserRepository extends DomainRepository<BlacklistUser> {

  Optional<BlacklistUser> findByUserId(UUID userId);
}
