package io.github.metdaisy.amaazon.auth.presentation.controller;

import io.github.metdaisy.amaazon.auth.application.dto.JwtLoginDto;
import io.github.metdaisy.amaazon.auth.application.dto.request.SignUpRequest;
import io.github.metdaisy.amaazon.auth.application.dto.request.UserCredentialUpdateRequest;
import io.github.metdaisy.amaazon.auth.application.service.AuthService;
import io.github.metdaisy.amaazon.auth.application.service.AuthTokenService;
import io.github.metdaisy.amaazon.auth.presentation.constant.AuthWebConstants;
import io.github.metdaisy.amaazon.auth.presentation.controller.dto.request.PasswordValidationRequest;
import io.github.metdaisy.amaazon.auth.presentation.provider.AuthCookieProvider;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthTokenService tokenService;
  private final AuthService service;
  private final AuthCookieProvider cookieProvider;

  @PostMapping("/refresh")
  public ResponseEntity<JwtLoginDto> refresh(
      @CookieValue(value = AuthWebConstants.REFRESH_TOKEN, required = false) String token) {
    if (!StringUtils.hasText(token)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JwtLoginDto loginDto = tokenService.reissue(token);
    ResponseCookie refreshTokenCookie = cookieProvider.createRefreshTokenCookie(
        loginDto.refreshToken());
    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(loginDto);
  }

  @PostMapping("/password/verify")
  public ResponseEntity<Void> verifyPassword(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid PasswordValidationRequest request) {
    service.verifyPassword(principal.getId(), request.password());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @PostMapping("/update")
  public ResponseEntity<Void> update(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid UserCredentialUpdateRequest request) {
    service.update(principal.getId(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @PostMapping("/signup")
  public ResponseEntity<Void> signup(@RequestBody @Valid SignUpRequest request) {
    service.create(request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}
