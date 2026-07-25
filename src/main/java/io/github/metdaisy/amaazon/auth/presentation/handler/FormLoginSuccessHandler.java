package io.github.metdaisy.amaazon.auth.presentation.handler;

import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthService;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.application.service.GuestTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FormLoginSuccessHandler extends AbstractLoginSuccessHandler {

  private final ObjectMapper mapper;
  private final GuestTokenService guestTokenService;
  private final AuthService authService;

  public FormLoginSuccessHandler(AuthTokenService authTokenService,
      AuthCookieProvider authCookieProvider, ObjectMapper mapper,
      GuestTokenService guestTokenService, AuthService authService) {
    super(authTokenService, authCookieProvider);
    this.mapper = mapper;
    this.guestTokenService = guestTokenService;
    this.authService = authService;
  }

  @Override
  protected String getDeviceId(HttpServletRequest request) {
    return request.getHeader(AuthWebConstants.HEADER_DEVICE_ID);
  }

  @Override
  protected void onSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication, JwtLoginDto loginDto) throws IOException {
    ResponseCookie guestTokenCookie = authCookieProvider.createDeleteGuestTokenCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, guestTokenCookie.toString());
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(response.getWriter(), loginDto);
    createSocialCredential(request, authentication);
  }

  private void createSocialCredential(HttpServletRequest request, Authentication authentication) {
    Cookie cookie = WebUtils.getCookie(request, AuthWebConstants.COOKIE_GUEST_TOKEN);
    if (cookie == null) {
      return;
    }
    String token = cookie.getValue();
    String provider = guestTokenService.getProvider(token);
    String providerId = guestTokenService.getProviderId(token);
    UUID userId = UUID.fromString(authentication.getName());
    authService.createSocial(userId, provider, providerId);
  }
}
