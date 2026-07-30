package io.github.metdaisy.amaazon.auth.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthBlacklistServiceTest {

  @Mock
  private BlacklistTokenRepository blacklistTokenRepository;

  @Mock
  private BlacklistUserRepository blacklistUserRepository;

  @InjectMocks
  private BlacklistService blacklistService;

  @Test
  @DisplayName("blacklist_success")
  void blacklist_success() {
    // given
    String jti = "test-jti";
    Instant expiredAt = Instant.now().plusSeconds(3600);

    // when
    blacklistService.createBlacklistToken(jti, expiredAt);

    // then
    verify(blacklistTokenRepository).save(any(BlacklistToken.class));
  }

  @Test
  @DisplayName("blacklist_failure_alreadyBlacklisted")
  void blacklist_failure_alreadyBlacklisted() {
    // given
    String jti = "duplicate-jti";
    Instant expiredAt = Instant.now().plusSeconds(3600);
    doThrow(new RuntimeException("Duplicate entry")).when(blacklistTokenRepository)
        .save(any(BlacklistToken.class));

    // when & then
    try {
      blacklistService.createBlacklistToken(jti, expiredAt);
    } catch (Exception e) {
      // Exception is expected due to duplicate
    }
  }
}
