package io.github.metdaisy.amaazon.address.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressCreateRequest(
    @NotBlank(message = "주소 별칭을 입력해주세요.")
    @Size(max = 100, message = "주소 별칭은 100자 이하로 입력해주세요.")
    String alias,
    @NotBlank(message = "수령인 이름을 입력해주세요.")
    @Size(max = 100, message = "수령인 이름은 100자 이하로 입력해주세요.")
    String recipientName,
    @NotBlank(message = "수령인 연락처를 입력해주세요.")
    @Size(max = 20, message = "수령인 연락처는 20자 이하로 입력해주세요.")
    String recipientPhone,
    @NotBlank(message = "우편번호를 입력해주세요.")
    @Size(max = 20, message = "우편번호는 20자 이하로 입력해주세요.")
    String postalCode,
    @NotBlank(message = "주소 본문을 입력해주세요.")
    @Size(max = 255, message = "주소 본문은 255자 이하로 입력해주세요.")
    String addressLine,
    boolean isPrimary) {
}
