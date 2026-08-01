package io.github.metdaisy.amaazon.auth.domain.event;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("signup")
public record SignUpTask(UUID id, String name, String phoneNumber, String address) {

}
