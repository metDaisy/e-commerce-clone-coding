package io.github.metdaisy.amaazon.auth.application.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import io.github.metdaisy.amaazon.global.security.jwt.registry.BlacklistRegistry;

@ExtendWith(MockitoExtension.class)
class BlacklistInitializerTest {

  @Mock
  private BlacklistRegistry registry;

  @Mock
  private BlacklistTokenRepository blacklistTokenRepository;

  @Mock
  private BlacklistUserRepository blacklistUserRepository;

  @InjectMocks
  private BlacklistInitializer initializer;

  @Test
  @DisplayName("handle ApplicationReadyEvent processes chunks correctly")
  @SuppressWarnings("unchecked")
  void handle() {
    // given
    Instant now = Instant.now();
    BlacklistToken token = BlacklistToken.of("token-123", now);
    Window<BlacklistToken> tokenWindow = mock(Window.class);
    when(tokenWindow.isEmpty()).thenReturn(false);
    when(tokenWindow.hasNext()).thenReturn(false);
    doAnswer(i -> {
      Consumer<BlacklistToken> c = i.getArgument(0);
      c.accept(token);
      return null;
    }).when(tokenWindow).forEach(any());

    when(blacklistTokenRepository.findTop1000By(any(ScrollPosition.class), any(Sort.class)))
        .thenReturn(tokenWindow);

    UUID userId = UUID.randomUUID();
    BlacklistUser user = BlacklistUser.of(userId, now);
    Window<BlacklistUser> userWindow = mock(Window.class);
    when(userWindow.isEmpty()).thenReturn(false);
    when(userWindow.hasNext()).thenReturn(false);
    doAnswer(i -> {
      Consumer<BlacklistUser> c = i.getArgument(0);
      c.accept(user);
      return null;
    }).when(userWindow).forEach(any());

    when(blacklistUserRepository.findTop1000By(any(ScrollPosition.class), any(Sort.class)))
        .thenReturn(userWindow);

    // when
    initializer.handle();

    // then
    verify(registry).blacklistToken("token-123", now);
    verify(registry).blacklistUser(userId, now);
  }
}
