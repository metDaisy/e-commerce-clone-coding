package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends DomainRepository<RefreshToken> {

  Optional<RefreshToken> findByToken(String token);
}
