package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.time.Instant;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;

public interface BlacklistUserRepository extends DomainRepository<BlacklistUser> {

  Window<BlacklistUser> findTop1000By(ScrollPosition position, Sort sort);

  void deleteByCompromisedAtLessThanEqual(Instant compromisedAt);
}
