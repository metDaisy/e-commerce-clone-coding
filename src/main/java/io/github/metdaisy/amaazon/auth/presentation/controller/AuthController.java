package io.github.metdaisy.amaazon.auth.presentation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthTokenService service;
  private final AuthCookieProvider cookieProvider;

  @PostMapping("/refresh")
  public ResponseEntity<JwtLoginDto> refresh(
      @CookieValue(value = AuthWebConstants.REFRESH_TOKEN, required = false) String token) {
    if (!StringUtils.hasText(token)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JwtLoginDto loginDto = service.reissue(token);
    ResponseCookie refreshTokenCookie = cookieProvider.createRefreshTokenCookie(loginDto.refreshToken());
    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(loginDto);
  }
}
