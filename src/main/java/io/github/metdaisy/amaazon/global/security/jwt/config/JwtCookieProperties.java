package io.github.metdaisy.amaazon.global.security.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.modulith.NamedInterface;

@NamedInterface("jwt")
@ConfigurationProperties(prefix = "amaazon.jwt.refresh.cookie")
public record JwtCookieProperties(String name, String path, String sameSite, boolean secure) {

}
