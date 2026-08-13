package io.github.metdaisy.amaazon.user.application.event;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("signup")
public record FormSignUpTask(UUID id, String name, String phoneNumber) {

}
