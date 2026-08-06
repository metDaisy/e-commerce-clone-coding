package io.github.metdaisy.amaazon.auth.presentation.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.event.FormLoginSuccessEvent;
import io.github.metdaisy.amaazon.auth.application.event.SocialSignUpTask;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper mapper;
  private final AuthCookieProvider cookieProvider;
  private final AuthTokenService tokenService;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    response.setStatus(HttpStatus.OK.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    String deviceId = request.getHeader(AuthWebConstants.HEADER_DEVICE_ID);
    UUID userId = UUID.fromString(authentication.getName());
    JwtLoginDto loginDto = tokenService.create(userId, deviceId);
    publishLoginSuccessEvent(userId);
    publishSocialSignUpTask(request, userId);

    response.addHeader(HttpHeaders.SET_COOKIE,
        cookieProvider.createDeleteGuestTokenCookie().toString());
    response.addHeader(HttpHeaders.SET_COOKIE,
        cookieProvider.createRefreshTokenCookie(loginDto.refreshToken()).toString());
    mapper.writeValue(response.getWriter(), loginDto);
  }

  private void publishLoginSuccessEvent(UUID userId) {
    eventPublisher.publishEvent(new FormLoginSuccessEvent(userId));
  }

  private void publishSocialSignUpTask(HttpServletRequest request, UUID userId) {
    Cookie cookie = WebUtils.getCookie(request, AuthWebConstants.COOKIE_GUEST_TOKEN);
    if (cookie == null) {
      return;
    }
    String guestToken = cookie.getValue();
    eventPublisher.publishEvent(new SocialSignUpTask(userId, guestToken));
  }
}
