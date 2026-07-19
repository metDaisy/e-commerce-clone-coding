package io.github.metdaisy.amaazon.auth.application.dto;

import java.util.UUID;

public record AuthUserDto(UUID userId, String role) {

}
