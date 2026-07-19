package io.github.metdaisy.amaazon.user.application.dto;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("user-api")
public record UserDto(UUID id, String role) {

}
