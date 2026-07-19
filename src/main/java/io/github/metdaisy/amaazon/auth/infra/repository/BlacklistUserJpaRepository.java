package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistUserJpaRepository extends JpaRepository<BlacklistUser, UUID>,
        BlacklistUserRepository {

}
