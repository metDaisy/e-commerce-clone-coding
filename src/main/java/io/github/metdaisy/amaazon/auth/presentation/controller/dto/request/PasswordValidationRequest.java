package io.github.metdaisy.amaazon.auth.presentation.controller.dto.request;

import io.github.metdaisy.amaazon.auth.application.validator.ValidPassword;

public record PasswordValidationRequest(@ValidPassword String password) {

}
