package io.github.metdaisy.amaazon.auth.presentation.controller;

import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.service.JwtTokenService;
import io.github.metdaisy.amaazon.auth.presentation.provider.JwtCookieProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final JwtTokenService service;
  private final JwtCookieProvider cookieProvider;

  @PostMapping("/refresh")
  public ResponseEntity<JwtLoginDto> refresh(
          @CookieValue(value = "REFRESH_TOKEN", required = false) String token) {
    if (!StringUtils.hasText(token)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JwtLoginDto loginDto = service.reissue(token);
    ResponseCookie cookie = cookieProvider.createTokenCookie(loginDto.refreshToken());
    return ResponseEntity.status(HttpStatus.OK)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(loginDto);
  }
}
