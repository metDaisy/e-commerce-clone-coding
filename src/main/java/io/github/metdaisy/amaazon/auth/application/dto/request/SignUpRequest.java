package io.github.metdaisy.amaazon.auth.application.dto.request;

import io.github.metdaisy.amaazon.auth.application.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
    @NotBlank @Pattern(
        regexp = "^[가-힣a-zA-Z]{1,10}$",
        message = "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요.") String name,
    @NotBlank @Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "올바른 이메일 형식이 아닙니다.") String email,
    @ValidPassword String password,
    @Pattern(regexp = "^\\d{11}$", message = "전화번호는 숫자만 11자리로 입력해주세요.") String phoneNumber,
    String address) {

}
