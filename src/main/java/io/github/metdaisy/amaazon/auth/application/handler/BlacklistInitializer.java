package io.github.metdaisy.amaazon.auth.application.handler;

import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import io.github.metdaisy.amaazon.global.security.jwt.registry.JwtRegistry;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BlacklistInitializer {

  private final JwtRegistry registry;
  private final BlacklistTokenRepository blacklistTokenRepository;
  private final BlacklistUserRepository blacklistUserRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void handle() {
    Sort tokenSort = Sort.by("token");
    processInChunks(
            position -> blacklistTokenRepository.findTop1000By(position, tokenSort),
            token -> registry.blacklistToken(token.getToken(), token.getExpiredAt())
    );

    Sort userSort = Sort.by("userId");
    processInChunks(
            position -> blacklistUserRepository.findTop1000By(position, userSort),
            user -> registry.blacklistUser(user.getUserId(), user.getCompromisedAt())
    );
  }

  private <T> void processInChunks(
          Function<ScrollPosition, Window<T>> fetcher,
          Consumer<T> processor) {
    ScrollPosition position = ScrollPosition.keyset();
    while (true) {
      Window<T> window = fetcher.apply(position);
      if (window.isEmpty()) {
        break;
      }
      window.forEach(processor);
      if (!window.hasNext()) {
        break;
      }
      position = window.positionAt(window.size() - 1);
    }
  }
}
