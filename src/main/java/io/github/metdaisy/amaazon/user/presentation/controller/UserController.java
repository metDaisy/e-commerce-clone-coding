package io.github.metdaisy.amaazon.user.presentation.controller;

import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.application.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequireEnabledUser
@RequiredArgsConstructor
public class UserController {

  private final UserService service;

  @PatchMapping
  public ResponseEntity<UserResponse> update(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid UserUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(service.update(principal.getId(), request));
  }

  @GetMapping
  public ResponseEntity<UserResponse> getMe(
      @AuthenticationPrincipal AmaazonPrincipal principal) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(service.findProfile(principal.getId()));
  }

  @PostMapping("/deactivate")
  public ResponseEntity<Void> deactivate(
      @AuthenticationPrincipal AmaazonPrincipal principal) {
    service.deactivate(principal.getId());
    return ResponseEntity.noContent().build();
  }
}
