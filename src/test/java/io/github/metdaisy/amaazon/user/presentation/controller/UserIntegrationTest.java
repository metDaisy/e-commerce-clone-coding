package io.github.metdaisy.amaazon.user.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@DisplayName("User HTTP 통합 테스트")
class UserIntegrationTest extends BaseIntegrationTest {

  private static final String PROFILE_URL = "/api/v1/me";
  private static final UUID USER_ID =
      UUID.fromString("2bb8df7f-9478-4d51-b055-496016dd421f");

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("프로필 조회 성공: HTTP 요청으로 DB의 사용자 프로필을 조회해 반환한다")
  void getProfile_readsFromDatabaseAndReturnsResponse() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    clear();

    // when & then
    mockMvc.perform(get(PROFILE_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.name").value("tester"))
        .andExpect(jsonPath("$.phoneNumber").value("01012345678"))
        .andExpect(jsonPath("$.roles[0]").value("USER"))
        .andExpect(jsonPath("$.isEnabled").value(true));
  }

  @Test
  @DisplayName("프로필 수정 성공: HTTP 요청으로 DB를 수정하고 변경된 프로필을 반환한다")
  void updateProfile_updatesDatabaseAndReturnsResponse() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    clear();
    UserUpdateRequest request = new UserUpdateRequest("updated", "01098765432");

    // when
    mockMvc.perform(patch(PROFILE_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.name").value(request.name()))
        .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()));
    flushAndClear();

    // then
    User updatedUser = userRepository.findById(USER_ID).orElseThrow();
    assertThat(updatedUser)
        .extracting(User::getName, User::getPhoneNumber)
        .containsExactly(request.name(), request.phoneNumber());
  }

  @Test
  @DisplayName("계정 비활성화 성공: HTTP 요청으로 DB의 활성 상태를 false로 변경한다")
  void deactivateAccount_updatesDatabase() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    clear();

    // when
    mockMvc.perform(post(PROFILE_URL + "/deactivate")
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isNoContent());
    flushAndClear();

    // then
    assertThat(userRepository.findById(USER_ID).orElseThrow().isEnabled())
        .isFalse();
  }

  private Authentication authenticationAs(UUID userId) {
    JwtPrincipal principal = new JwtPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
  }
}
