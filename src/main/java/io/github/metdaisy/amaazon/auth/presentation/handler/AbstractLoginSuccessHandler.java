package io.github.metdaisy.amaazon.auth.presentation.handler;

import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.JwtTokenService;
import io.github.metdaisy.amaazon.auth.presentation.provider.JwtCookieProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@RequiredArgsConstructor
public abstract class AbstractLoginSuccessHandler implements AuthenticationSuccessHandler {

  protected final JwtTokenService jwtTokenService;
  protected final JwtCookieProvider jwtCookieProvider;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication) throws IOException, ServletException {
          
    // 1. 게스트 검증 훅 (필요한 경우 하위 클래스에서 처리 후 true 반환 시 조기 종료)
    if (handleGuest(request, response, authentication)) {
        return;
    }

    // 2. 유저 식별자 추출
    UUID userId = UUID.fromString(authentication.getName());
    
    // 3. 기기 식별자 추출 (추상 메서드 - 자식 클래스에서 구현)
    String deviceId = getDeviceId(request);

    // 4. JWT 토큰 및 Refresh 토큰 쿠키 발급 (공통 로직)
    JwtLoginDto loginDto = jwtTokenService.create(userId, deviceId);
    response.addHeader(HttpHeaders.SET_COOKIE, jwtCookieProvider.createTokenCookie(
            loginDto.refreshToken()).toString());

    // 5. 최종 응답 처리 (추상 메서드 - 자식 클래스에서 구현)
    onSuccess(request, response, authentication, loginDto);
  }

  // 기본적으로 false를 반환하며, 
  // 소셜 로그인 등 게스트 판별이 필요한 자식 클래스에서만 오버라이드하여 리다이렉트 처리합니다.
  protected boolean handleGuest(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication) throws IOException {
      return false;
  }

  protected abstract String getDeviceId(HttpServletRequest request);

  protected abstract void onSuccess(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication, JwtLoginDto loginDto) throws IOException;
}
