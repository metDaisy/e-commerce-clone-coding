package io.github.metdaisy.amaazon.global.security.jwt.registry;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * <h3>BlacklistRegistry</h3>
 * json web token 은 stateless 특징을 가진다.
 * <p>
 * jwt 를 발급하면 유효기간 동안 인증에서 자유롭다. 만약 해커가 탈취한다면 서버는 이게 탈취된 것인지 모른다. 또한 유저가 정상적으로 로그아웃해도 토큰이 유효하다면 사용할
 * 수 있다. 이러한 문제를 해결하기 위해 토큰을 못쓰게 만드는 기능을 구현 하였다.
 */
@NamedInterface("jwt")
public interface BlacklistRegistry {

  /**
   * @param jti       access token 에서 jwt id 를 가져온다.
   * @param expiredAt access token expiration time
   */
  void blacklistToken(String jti, Instant expiredAt);

  /**
   * @param userId user id
   * @param compromisedAt token 을 탈취 당한 것을 안 시각.
   */
  void blacklistUser(UUID userId, Instant compromisedAt);

  /**
   * @param userId user id
   * @param jti access token jwt id
   * @param issuedAt access token 발행 시각
   * */
  boolean isBlacklisted(String jti, UUID userId, Instant issuedAt);

}
