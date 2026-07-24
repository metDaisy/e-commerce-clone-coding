package io.github.metdaisy.amaazon.auth.presentation.handler;

import io.github.metdaisy.amaazon.auth.application.service.GuestTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;

@Component
public class FormLoginSuccessHandler extends AbstractLoginSuccessHandler {

  private final ObjectMapper mapper;
  private final GuestTokenService guestTokenService;

  public FormLoginSuccessHandler(AuthTokenService authTokenService,
      AuthCookieProvider authCookieProvider, ObjectMapper mapper,
          GuestTokenService guestTokenService) {
    super(authTokenService, authCookieProvider);
    this.mapper = mapper;
    this.guestTokenService = guestTokenService;
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
  }

  private void createSocialCredential(HttpServletRequest request, Authentication authentication) {
    Cookie cookie = WebUtils.getCookie(request, AuthWebConstants.COOKIE_GUEST_TOKEN);
    if (cookie == null) {
      return;
    }

  }
}
