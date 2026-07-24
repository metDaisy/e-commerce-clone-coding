package io.github.metdaisy.amaazon.auth.infra.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;

public enum OAuth2Provider implements Converter<Map<String, Object>, Map<String, Object>> {
    GOOGLE {
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>();
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(attributes.getOrDefault("sub", ""))); // 구글의 고유 식별자
            normalized.put("email", attributes.get("email"));
            normalized.put("name", attributes.get("name"));
            return normalized;
        }
    },
    NAVER {
        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>();
            Map<String, Object> response =
                    (Map<String, Object>) attributes.getOrDefault("response", Collections.emptyMap());
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(response.getOrDefault("id", ""))); // 네이버의 고유 식별자
            normalized.put("email", response.get("email"));
            normalized.put("name", response.getOrDefault("name", response.get("nickname")));

            return normalized;
        }
    },
    KAKAO {
        @Override
        public Map<String, Object> convert(Map<String, Object> attributes) {
            Map<String, Object> normalized = new HashMap<>();
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(attributes.getOrDefault("id", ""))); // 카카오의 고유 식별자 (Long 타입)
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
            Map<String, Object> normalized = new HashMap<>();
            normalized.put("provider", getProvider());
            normalized.put("providerId", String.valueOf(attributes.getOrDefault("id", ""))); // 깃허브의 고유 식별자 (Integer)
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
                        Map.of("provider", registrationId)));
    }
}
