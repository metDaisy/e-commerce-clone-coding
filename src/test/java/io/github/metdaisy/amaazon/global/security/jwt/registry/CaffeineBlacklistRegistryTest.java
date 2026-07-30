package io.github.metdaisy.amaazon.global.security.jwt.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistTokenCreatedEvent;
import io.github.metdaisy.amaazon.global.security.jwt.event.BlacklistUserCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CaffeineBlacklistRegistryTest {

  @Mock
  private Cache<String, Instant> tokenBlacklist;

  @Mock
  private Cache<UUID, Instant> userBlacklist;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Captor
  private ArgumentCaptor<BlacklistTokenCreatedEvent> tokenEventCaptor;

  @Captor
  private ArgumentCaptor<BlacklistUserCreatedEvent> userEventCaptor;

  private CaffeineBlacklistRegistry blacklistRegistry;

  @BeforeEach
  void setUp() {
    blacklistRegistry = new CaffeineBlacklistRegistry(tokenBlacklist, userBlacklist, eventPublisher);
  }

  @DisplayName("토큰 블랙리스트 등록 - 성공: 토큰 JTI 가 캐시에 저장되고 이벤트가 발행된다")
  @Test
  void blacklistToken_success() {
    // given
    String jti = "test-jti-123";
    Instant expiredAt = Instant.now().plusSeconds(3600);

    // when
    blacklistRegistry.blacklistToken(jti, expiredAt);

    // then
    then(tokenBlacklist).should().put(jti, expiredAt);
    then(eventPublisher).should().publishEvent(tokenEventCaptor.capture());
    BlacklistTokenCreatedEvent capturedEvent = tokenEventCaptor.getValue();
    assertThat(capturedEvent.jti()).isEqualTo(jti);
    assertThat(capturedEvent.expiredAt()).isEqualTo(expiredAt);
  }

  @DisplayName("사용자 블랙리스트 등록 - 성공: 사용자 ID 가 캐시에 저장되고 이벤트가 발행된다")
  @Test
  void blacklistUser_success() {
    // given
    UUID userId = UUID.randomUUID();
    Instant compromisedAt = Instant.now();

    // when
    blacklistRegistry.blacklistUser(userId, compromisedAt);

    // then
    then(userBlacklist).should().put(userId, compromisedAt);
    then(eventPublisher).should().publishEvent(userEventCaptor.capture());
    BlacklistUserCreatedEvent capturedEvent = userEventCaptor.getValue();
    assertThat(capturedEvent.userId()).isEqualTo(userId);
    assertThat(capturedEvent.compromisedAt()).isEqualTo(compromisedAt);
  }

  @DisplayName("블랙리스트 확인 - 성공: 토큰 JTI 가 블랙리스트에 있으면 true 를 반환한다")
  @Test
  void isBlacklisted_tokenBlacklisted_returnsTrue() {
    // given
    String jti = "blacklisted-jti";
    Instant expiredAt = Instant.now().plusSeconds(3600);
    given(tokenBlacklist.getIfPresent(jti)).willReturn(expiredAt);

    UUID userId = UUID.randomUUID();
    Instant issuedAt = Instant.now();

    // when
    boolean result = blacklistRegistry.isBlacklisted(jti, userId, issuedAt);

    // then
    assertThat(result).isTrue();
    then(userBlacklist).should(never()).getIfPresent(any());
  }

  @DisplayName("블랙리스트 확인 - 성공: 사용자 ID 가 블랙리스트에 있고 토큰 발급 시간이 사용자 블랙리스트 등록 시간 이전이면 true 를 반환한다")
  @Test
  void isBlacklisted_userBlacklisted_issuedBefore_returnsTrue() {
    // given
    String jti = "valid-jti";
    given(tokenBlacklist.getIfPresent(jti)).willReturn(null);

    UUID userId = UUID.randomUUID();
    Instant compromisedAt = Instant.now().minusSeconds(3600);
    given(userBlacklist.getIfPresent(userId)).willReturn(compromisedAt);

    Instant issuedAt = Instant.now().minusSeconds(7200);

    // when
    boolean result = blacklistRegistry.isBlacklisted(jti, userId, issuedAt);

    // then
    assertThat(result).isTrue();
  }

  @DisplayName("블랙리스트 확인 - 실패: 토큰 JTI 와 사용자 ID 가 모두 블랙리스트에 없으면 false 를 반환한다")
  @Test
  void isBlacklisted_notBlacklisted_returnsFalse() {
    // given
    String jti = "valid-jti";
    given(tokenBlacklist.getIfPresent(jti)).willReturn(null);

    UUID userId = UUID.randomUUID();
    given(userBlacklist.getIfPresent(userId)).willReturn(null);

    Instant issuedAt = Instant.now();

    // when
    boolean result = blacklistRegistry.isBlacklisted(jti, userId, issuedAt);

    // then
    assertThat(result).isFalse();
  }

  @DisplayName("블랙리스트 확인 - 실패: 사용자 ID 가 블랙리스트에 있지만 토큰 발급 시간이 등록 시간 이후이면 false 를 반환한다")
  @Test
  void isBlacklisted_userBlacklisted_issuedAfter_returnsFalse() {
    // given
    String jti = "valid-jti";
    given(tokenBlacklist.getIfPresent(jti)).willReturn(null);

    UUID userId = UUID.randomUUID();
    Instant compromisedAt = Instant.now().minusSeconds(3600);
    given(userBlacklist.getIfPresent(userId)).willReturn(compromisedAt);

    Instant issuedAt = Instant.now();

    // when
    boolean result = blacklistRegistry.isBlacklisted(jti, userId, issuedAt);

    // then
    assertThat(result).isFalse();
  }
}
