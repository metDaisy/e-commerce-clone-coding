package io.github.metdaisy.amaazon.user.application.port.out;

import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public interface UserLoginEmailQuery {

  Optional<String> findByUserId(UUID userId);
}
