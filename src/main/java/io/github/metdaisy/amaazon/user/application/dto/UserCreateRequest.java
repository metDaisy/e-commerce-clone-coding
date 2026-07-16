package io.github.metdaisy.amaazon.user.application.dto;

import jakarta.validation.constraints.Pattern;

public record UserCreateRequest(
        @Pattern(regexp = "^[가-힣a-zA-Z]{1,10}$",
                message = "이름은 영문자 또는 한글만 1자 이상 10자 이하로 입력해주세요.")
        String name,
        @Pattern(regexp = "^\\d{11}$", message = "전화번호는 숫자만 11자리로 입력해주세요.")
        String phoneNumber,
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "올바른 이메일 형식이 아닙니다.")
        String email,
        @Pattern(regexp = "^(?!.*\\s)(?!.*\\|).{6,13}$",
                message = "비밀번호는 공백과 |를 포함할 수 없으며 6~13자여야 합니다.")
        String password) {

}

