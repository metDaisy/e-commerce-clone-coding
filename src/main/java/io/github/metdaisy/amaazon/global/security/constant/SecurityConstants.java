package io.github.metdaisy.amaazon.global.security.constant;

import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

  // Login Parameters
  public static final String USERNAME_PARAMETER = "username";
  public static final String PASSWORD_PARAMETER = "password";

  // API Endpoints
  public static final String LOGIN_URL = WebConstants.SERVLET_PREFIX + "/auth/login";
  public static final String LOGOUT_URL = WebConstants.SERVLET_PREFIX + "/auth/logout";
  public static final String REFRESH_URL = WebConstants.SERVLET_PREFIX + "/auth/refresh";
  public static final String USERS_URL = WebConstants.SERVLET_PREFIX + "/users";

  // Request Matchers
  public static final String[] PUBLIC_GET_PATHS = {
          "/ws/**",
          "/actuator/**",
          "/swagger-ui.html",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/api.json"
  };

  public static final String[] PUBLIC_POST_PATHS = {
          LOGIN_URL,
          LOGOUT_URL,
          REFRESH_URL,
          USERS_URL
  };
}
