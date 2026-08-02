package io.github.metdaisy.amaazon.global.outbox.scheduler;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

  private final IncompleteEventPublications incompleteEvents;
  private final CompletedEventPublications completedEvents;

  @Scheduled(fixedDelay = 60 * 1000)
  public void retryIncompleteEvents() {
    incompleteEvents.resubmitIncompletePublicationsOlderThan(Duration.ofSeconds(30));
  }

  @Scheduled(cron = "0 0 1 * * *")
  public void cleanUpCompletedEvents() {
    completedEvents.deletePublicationsOlderThan(Duration.ofDays(7));
  }
}
