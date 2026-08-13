package io.github.metdaisy.amaazon.user.application.dto.request;

import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest(
    @Pattern(
        regexp = "^[가-힣a-zA-Z]{1,10}$",
        message = "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요.") String name,
    @Pattern(regexp = "^\\d{11}$", message = "전화번호는 숫자만 11자리로 입력해주세요.") String phoneNumber) {

}
