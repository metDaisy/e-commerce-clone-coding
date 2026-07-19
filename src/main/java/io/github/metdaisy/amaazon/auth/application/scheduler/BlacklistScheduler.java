package io.github.metdaisy.amaazon.auth.application.scheduler;

import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistScheduler {

  private final BlacklistTokenRepository blacklistTokenRepository;
  private final BlacklistUserRepository blacklistUserRepository;

  @Value("${amaazon.jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  @Scheduled(cron = "0 0 * * * ?")
  @Transactional
  public void cleanupBlacklistToken() {
    Instant now = Instant.now();
    log.info("Starting blacklist token cleanup scheduler at {}", now);
    try {
      blacklistTokenRepository.deleteByExpiredAtLessThanEqual(now);
      log.info("Blacklist token cleanup completed successfully.");
    } catch (Exception e) {
      log.error("Error occurred during blacklist token cleanup", e);
    }
  }

  @Scheduled(cron = "0 0 0 * * ?")
  @Transactional
  public void cleanupBlacklistUser() {
    Instant now = Instant.now();
    log.info("Starting blacklist user cleanup scheduler at {}", now);
    try {
      Instant compromisedThreshold = now.minus(refreshTokenExpiration, ChronoUnit.SECONDS);
      blacklistUserRepository.deleteByCompromisedAtLessThanEqual(compromisedThreshold);
      log.info("Blacklist user cleanup completed successfully.");
    } catch (Exception e) {
      log.error("Error occurred during blacklist user cleanup", e);
    }
  }
}
