package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;

public interface BlacklistTokenRepository extends DomainRepository<BlacklistToken> {

  Optional<BlacklistToken> findByToken(String token);
}
