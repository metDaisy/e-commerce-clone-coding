package io.github.metdaisy.amaazon.auth.presentation.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper mapper;
  private final JwtTokenService service;
  private final JwtCookieProvider provider;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
          Authentication authentication) throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    String device = request.getHeader("X-Device-Id");
    UUID userId = UUID.fromString(authentication.getName());
    JwtLoginDto loginDto = service.create(userId, device);
    response.addHeader(HttpHeaders.SET_COOKIE, provider.createTokenCookie(
            loginDto.refreshToken()).toString());
    mapper.writeValue(response.getWriter(), loginDto);
  }
}
