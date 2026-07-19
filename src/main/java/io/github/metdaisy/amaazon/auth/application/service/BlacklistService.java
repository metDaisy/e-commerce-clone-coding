package io.github.metdaisy.amaazon.auth.application.service;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BlacklistService {

  private final BlacklistTokenRepository blacklistTokenRepository;
  private final BlacklistUserRepository blacklistUserRepository;

  public void createBlacklistToken(String jti, Instant instant) {
    BlacklistToken blacklistToken = BlacklistToken.of(jti, instant);
    blacklistTokenRepository.save(blacklistToken);
  }

  public void createBlacklistUser(UUID userId, Instant instant) {
    BlacklistUser blacklistUser = BlacklistUser.of(userId, instant);
    blacklistUserRepository.save(blacklistUser);
  }
}
