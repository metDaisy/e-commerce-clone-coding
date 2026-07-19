package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistTokenJpaRepository extends JpaRepository<BlacklistToken, UUID>,
        BlacklistTokenRepository {

}
