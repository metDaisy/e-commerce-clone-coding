package io.github.metdaisy.amaazon.common.auth;

import java.util.UUID;

public interface AmaazonPrincipal {

  UUID getId();

  String getRole();
}
