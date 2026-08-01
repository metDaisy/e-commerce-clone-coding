package io.github.metdaisy.amaazon.auth.application.dto.request;

import io.github.metdaisy.amaazon.auth.application.validator.ValidPassword;
import jakarta.validation.constraints.Pattern;

public record UserCredentialUpdateRequest(
    @Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "올바른 이메일 형식이 아닙니다.") String email,
    @ValidPassword(required = false) String password) {

}
