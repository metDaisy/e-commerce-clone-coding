package io.github.metdaisy.amaazon.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthErrorCode;
import io.github.metdaisy.amaazon.auth.domain.exception.AuthException;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("소셜 자격증명 서비스 테스트")
class SocialCredentialServiceTest {

  @Mock
  private SocialCredentialRepository repository;
  @Mock
  private UserQueryApi userQueryApi;

  @InjectMocks
  private SocialCredentialService socialCredentialService;

  @Test
  @DisplayName("create - 성공")
  void create_success() {
    // given
    UUID userId = UUID.randomUUID();
    given(userQueryApi.existsEnabledUser(userId)).willReturn(true);

    // when
    socialCredentialService.create(userId, "google", "provider-123");

    // then
    ArgumentCaptor<SocialCredential> captor = ArgumentCaptor.forClass(SocialCredential.class);
    verify(repository).save(captor.capture());
    
    SocialCredential saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getProvider()).isEqualTo("google");
    assertThat(saved.getProviderId()).isEqualTo("provider-123");
  }

  @Test
  @DisplayName("create - 실패: 사용자가 존재하지 않음")
  void create_failure_user_not_found() {
    UUID userId = UUID.randomUUID();
    given(userQueryApi.existsEnabledUser(userId)).willReturn(false);

    assertThatThrownBy(() -> socialCredentialService.create(userId, "google", "provider-123"))
        .isInstanceOf(AuthException.class)
        .hasFieldOrPropertyWithValue("code", AuthErrorCode.USER_NOT_FOUND.getCode());

    verify(repository, never()).save(any());
  }
}
