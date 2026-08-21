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
import org.springframework.test.web.servlet.ResultActions;

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
  @DisplayName("프로필 수정 성공: 연락처를 생략하면 이름만 변경한다")
  void updateProfile_updatesOnlyNameWhenPhoneNumberIsOmitted() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    clear();
    UserUpdateRequest request = new UserUpdateRequest("updated", null);

    // when
    mockMvc.perform(patch(PROFILE_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(request.name()))
        .andExpect(jsonPath("$.phoneNumber").value("01012345678"));
    flushAndClear();

    // then
    User updatedUser = userRepository.findById(USER_ID).orElseThrow();
    assertThat(updatedUser)
        .extracting(User::getName, User::getPhoneNumber)
        .containsExactly(request.name(), "01012345678");
  }

  @Test
  @DisplayName("프로필 수정 성공: 이름을 생략하면 연락처만 변경한다")
  void updateProfile_updatesOnlyPhoneNumberWhenNameIsOmitted() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    clear();
    UserUpdateRequest request = new UserUpdateRequest(null, "01098765432");

    // when
    mockMvc.perform(patch(PROFILE_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("tester"))
        .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()));
    flushAndClear();

    // then
    User updatedUser = userRepository.findById(USER_ID).orElseThrow();
    assertThat(updatedUser)
        .extracting(User::getName, User::getPhoneNumber)
        .containsExactly("tester", request.phoneNumber());
  }

  @Test
  @DisplayName("프로필 수정 실패: 다른 User가 사용 중인 이름이면 409를 반환한다")
  void updateProfile_rejectsDuplicateName() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    persistAndFlush(User.createUser(UUID.randomUUID(), "duplicate", "01011112222"));
    clear();
    UserUpdateRequest request = new UserUpdateRequest("duplicate", null);

    // when
    ResultActions result = mockMvc.perform(patch(PROFILE_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

    // then
    result
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("USER-002"));
    flushAndClear();

    // then
    User unchangedUser = userRepository.findById(USER_ID).orElseThrow();
    assertThat(unchangedUser)
        .extracting(User::getName, User::getPhoneNumber)
        .containsExactly("tester", "01012345678");
  }

  @Test
  @DisplayName("프로필 수정 실패: 다른 User가 사용 중인 연락처면 409를 반환한다")
  void updateProfile_rejectsDuplicatePhoneNumber() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    persistAndFlush(User.createUser(UUID.randomUUID(), "other", "01098765432"));
    clear();
    UserUpdateRequest request = new UserUpdateRequest(null, "01098765432");

    // when
    ResultActions result = mockMvc.perform(patch(PROFILE_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

    // then
    result
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.exceptionCode").value("USER-003"));
    flushAndClear();

    // then
    User unchangedUser = userRepository.findById(USER_ID).orElseThrow();
    assertThat(unchangedUser)
        .extracting(User::getName, User::getPhoneNumber)
        .containsExactly("tester", "01012345678");
  }

  @Test
  @DisplayName("프로필 수정 성공: 비활성 User의 이름과 연락처는 재사용할 수 있다")
  void updateProfile_allowsIdentifiersUsedByDisabledUser() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    User disabledUser = User.createUser(UUID.randomUUID(), "disabled", "01098765432");
    persistAndFlush(disabledUser);
    disabledUser.deactivate();
    flushAndClear();
    UserUpdateRequest request = new UserUpdateRequest("disabled", "01098765432");

    // when
    ResultActions result = mockMvc.perform(patch(PROFILE_URL)
        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(request.name()))
        .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()));
    flushAndClear();

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
