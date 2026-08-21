package io.github.metdaisy.amaazon.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.UUID;

public record JwtLoginDto(
    UUID userId,
    List<String> roles,
    String accessToken,
    @JsonIgnore String refreshToken) {

}
