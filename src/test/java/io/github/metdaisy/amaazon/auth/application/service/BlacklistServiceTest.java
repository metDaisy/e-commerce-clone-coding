package io.github.metdaisy.amaazon.auth.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

  @Mock
  private BlacklistTokenRepository blacklistTokenRepository;
  
  @Mock
  private BlacklistUserRepository blacklistUserRepository;

  @InjectMocks
  private BlacklistService blacklistService;

  @Test
  @DisplayName("createBlacklistToken")
  void createBlacklistToken() {
    Instant now = Instant.now();
    blacklistService.createBlacklistToken("jti-123", now);
    verify(blacklistTokenRepository).save(any(BlacklistToken.class));
  }

  @Test
  @DisplayName("createBlacklistUser")
  void createBlacklistUser() {
    Instant now = Instant.now();
    UUID userId = UUID.randomUUID();
    blacklistService.createBlacklistUser(userId, now);
    verify(blacklistUserRepository).save(any(BlacklistUser.class));
  }
}
