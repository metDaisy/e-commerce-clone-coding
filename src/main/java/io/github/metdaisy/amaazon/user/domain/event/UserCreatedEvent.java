package io.github.metdaisy.amaazon.user.domain.event;

import java.util.UUID;

public record UserCreatedEvent(UUID id, String email, String password) {

}
