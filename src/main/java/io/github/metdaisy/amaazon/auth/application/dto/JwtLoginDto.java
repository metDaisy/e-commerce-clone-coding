package io.github.metdaisy.amaazon.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

public record JwtLoginDto(UUID userId, String accessToken, @JsonIgnore String refreshToken) {

}
