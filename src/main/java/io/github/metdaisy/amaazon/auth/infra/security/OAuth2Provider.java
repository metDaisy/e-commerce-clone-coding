package io.github.metdaisy.amaazon.auth.infra.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.common.exception.AmaazonExceptionContext;

public enum OAuth2Provider implements Converter<Map<String, Object>, Map<String, Object>> {
    GOOGLE {
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>(attributes);
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(attributes.getOrDefault("sub", "")));
            return normalized;
        }
    },
    NAVER {
        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>(attributes);
            Map<String, Object> response =
                    (Map<String, Object>) attributes.getOrDefault("response", Collections.emptyMap());
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(response.getOrDefault("id", "")));
            normalized.put("email", response.get("email"));
            normalized.put("name", response.getOrDefault("name", response.get("nickname")));
            return normalized;
        }
    },
    KAKAO {
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>(attributes);
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(attributes.getOrDefault("id", "")));
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount =
                    (Map<String, Object>) attributes.getOrDefault("kakao_account", Collections.emptyMap());
            normalized.put("email", kakaoAccount.getOrDefault("email", null));
            @SuppressWarnings("unchecked")
            Map<String, Object> profile =
                    (Map<String, Object>) kakaoAccount.getOrDefault("profile", Collections.emptyMap());
            normalized.put("name", profile.getOrDefault("nickname", null));
            return normalized;
        }
    },
    GITHUB {
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>(attributes);
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(attributes.getOrDefault("id", "")));
            normalized.put("email", attributes.getOrDefault("email", null));
            normalized.put("name", attributes.getOrDefault("login", null));
            return normalized;
        }
    };

    public String getProvider() {
        return name();
    }

    public static OAuth2Provider from(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.getProvider().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNSUPPORTED_PROVIDER,
                        AmaazonExceptionContext.logDetails(Map.of("provider", registrationId))));
    }
}
