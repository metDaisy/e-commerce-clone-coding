package io.github.metdaisy.amaazon.common.auth;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface
public interface AmaazonPrincipal {

  UUID getId();

  String getRole();
}
