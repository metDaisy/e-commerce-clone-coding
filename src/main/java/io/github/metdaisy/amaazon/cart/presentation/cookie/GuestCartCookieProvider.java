package io.github.metdaisy.amaazon.cart.presentation.cookie;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class GuestCartCookieProvider {

  public static final String NAME = "guest_cart_id";
  private final boolean secure;

  public GuestCartCookieProvider(
      @Value("${amaazon.cart.guest-cookie.secure:false}") boolean secure) {
    this.secure = secure;
  }

  public ResponseCookie create(String cartId) {
    return ResponseCookie.from(NAME, cartId)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofDays(30))
        .build();
  }

  public ResponseCookie delete() {
    return ResponseCookie.from(NAME, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ZERO)
        .build();
  }
}
