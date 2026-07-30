package io.github.metdaisy.amaazon.auth.application.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;

@ExtendWith(MockitoExtension.class)
class BlacklistSchedulerTest {

  @Mock
  private BlacklistTokenRepository blacklistTokenRepository;

  @Mock
  private BlacklistUserRepository blacklistUserRepository;

  @Spy
  private Clock clock = Clock.fixed(Instant.parse("2026-07-30T10:00:00Z"), ZoneId.of("UTC"));

  @InjectMocks
  private BlacklistScheduler scheduler;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(scheduler, "refreshTokenExpiration", 3600L);
  }

  @Test
  @DisplayName("cleanupBlacklistToken success")
  void cleanupBlacklistToken() {
    scheduler.cleanupBlacklistToken();
    verify(blacklistTokenRepository).deleteByExpiredAtLessThanEqual(any(Instant.class));
  }

  @Test
  @DisplayName("cleanupBlacklistToken handles exception")
  void cleanupBlacklistToken_exception() {
    doThrow(new RuntimeException("DB error")).when(blacklistTokenRepository)
        .deleteByExpiredAtLessThanEqual(any(Instant.class));

    // Should not throw
    scheduler.cleanupBlacklistToken();
    verify(blacklistTokenRepository).deleteByExpiredAtLessThanEqual(any(Instant.class));
  }

  @Test
  @DisplayName("cleanupBlacklistUser success")
  void cleanupBlacklistUser() {
    scheduler.cleanupBlacklistUser();
    verify(blacklistUserRepository).deleteByCompromisedAtLessThanEqual(any(Instant.class));
  }

  @Test
  @DisplayName("cleanupBlacklistUser handles exception")
  void cleanupBlacklistUser_exception() {
    doThrow(new RuntimeException("DB error")).when(blacklistUserRepository)
        .deleteByCompromisedAtLessThanEqual(any(Instant.class));

    // Should not throw
    scheduler.cleanupBlacklistUser();
    verify(blacklistUserRepository).deleteByCompromisedAtLessThanEqual(any(Instant.class));
  }
}
