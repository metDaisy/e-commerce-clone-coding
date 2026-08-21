package io.github.metdaisy.amaazon.address.application.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
    @Size(max = 100, message = "주소 별칭은 100자 이하로 입력해주세요.")
    @Pattern(regexp = ".*\\S.*", message = "must not be blank when provided")
    String alias,
    @Size(max = 100, message = "수령인 이름은 100자 이하로 입력해주세요.")
    @Pattern(regexp = ".*\\S.*", message = "must not be blank when provided")
    String recipientName,
    @Size(max = 20, message = "수령인 연락처는 20자 이하로 입력해주세요.")
    @Pattern(regexp = ".*\\S.*", message = "must not be blank when provided")
    String recipientPhone,
    @Size(max = 20, message = "우편번호는 20자 이하로 입력해주세요.")
    @Pattern(regexp = ".*\\S.*", message = "must not be blank when provided")
    String postalCode,
    @Size(max = 255, message = "주소 본문은 255자 이하로 입력해주세요.")
    @Pattern(regexp = ".*\\S.*", message = "must not be blank when provided")
    String addressLine) {
}
