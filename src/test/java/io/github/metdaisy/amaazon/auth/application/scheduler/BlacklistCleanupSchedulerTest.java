package io.github.metdaisy.amaazon.auth.application.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;

@ExtendWith(MockitoExtension.class)
class BlacklistCleanupSchedulerTest {

  @Mock
  private BlacklistTokenRepository blacklistTokenRepository;

  @Mock
  private BlacklistUserRepository blacklistUserRepository;

  private Clock clock;

  private BlacklistScheduler blacklistScheduler;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-07-30T10:00:00Z"), ZoneId.of("UTC"));
    blacklistScheduler = new BlacklistScheduler(blacklistTokenRepository, blacklistUserRepository, clock);
  }

  @Test
  @DisplayName("cleanup_success")
  void cleanup_success() {
    // given
    Instant now = Instant.now(clock);

    // when
    blacklistScheduler.cleanupBlacklistToken();

    // then
    verify(blacklistTokenRepository).deleteByExpiredAtLessThanEqual(now);
  }

  @Test
  @DisplayName("cleanup_failure_exception")
  void cleanup_failure_exception() {
    // given
    doThrow(new RuntimeException("DB error")).when(blacklistTokenRepository)
        .deleteByExpiredAtLessThanEqual(any(Instant.class));

    // when
    // scheduler catches exception and logs, does not throw
    blacklistScheduler.cleanupBlacklistToken();

    // then
    // No exception should be thrown, scheduler handles it internally
    verify(blacklistTokenRepository).deleteByExpiredAtLessThanEqual(any(Instant.class));
  }
}
