package io.github.metdaisy.amaazon.user.presentation.controller;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserAccountController {

  private final UserService service;

  @PostMapping("/me/deactivate")
  public ResponseEntity<Void> deactivate(
      @AuthenticationPrincipal AmaazonPrincipal principal) {
    service.deactivate(principal.getId());
    return ResponseEntity.noContent().build();
  }
}
