package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.time.Instant;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;

public interface BlacklistTokenRepository extends DomainRepository<BlacklistToken> {

  Window<BlacklistToken> findTop1000By(ScrollPosition position,Sort sort);

  void deleteByExpiredAtLessThanEqual(Instant expiredAt);
}
