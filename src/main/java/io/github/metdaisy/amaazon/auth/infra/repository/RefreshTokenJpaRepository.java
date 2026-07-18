package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.auth.domain.repository.RefreshTokenRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID>,
        RefreshTokenRepository {

}
