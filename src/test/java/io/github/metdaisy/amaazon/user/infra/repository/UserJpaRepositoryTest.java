package io.github.metdaisy.amaazon.user.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("User JPA 저장소 슬라이스 테스트")
class UserJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private UserJpaRepository repository;

  @Test
  @DisplayName("사용자 저장: User를 저장한 뒤 같은 식별자로 조회할 수 있다")
  void save_persistsUser() {
    // given
    User user = User.createUser(UUID.randomUUID(), "tester", "01011112222");

    // when
    User savedUser = repository.save(user);
    flushAndClear();

    // then
    Optional<User> persistedUser = repository.findWithRolesById(user.getId());
    assertThat(savedUser.getId()).isEqualTo(user.getId());
    assertUserFields(persistedUser.orElseThrow(), true);
    ensureQueryCount(1);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidUsers")
  @DisplayName("사용자 저장 실패: 제약조건 위반 User는 저장하지 않는다")
  void save_rejectsInvalidUser(String caseName, User user) {
    // when & then
    assertThatThrownBy(() -> {
      repository.save(user);
      em.flush();
    }).isInstanceOf(ConstraintViolationException.class);
  }

  private static Stream<Arguments> invalidUsers() {
    return Stream.of(
        Arguments.of("이름 null", User.createUser(UUID.randomUUID(), null, "01011112222")),
        Arguments.of("전화번호 null", User.createUser(UUID.randomUUID(), "tester", null)),
        Arguments.of("이름 10자 초과",
            User.createUser(UUID.randomUUID(), "12345678901", "01011112222")),
        Arguments.of("전화번호 11자 초과",
            User.createUser(UUID.randomUUID(), "tester", "010111122223")));
  }

  @Test
  @DisplayName("사용자 조회: 저장된 식별자로 User를 조회할 수 있다")
  void findById_returnsPersistedUser() {
    // given
    User user = User.createUser(UUID.randomUUID(), "tester", "01011112222");
    persistAndFlush(user);
    clear();

    // when
    User foundUser = repository.findById(user.getId()).orElseThrow();

    // then
    assertUserFields(foundUser, true);
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("역할 포함 사용자 조회: 명시적 Fetch로 User와 roles를 함께 조회한다")
  void findWithRolesById_returnsUserWithRoles() {
    // given
    User user = User.createUser(UUID.randomUUID(), "tester", "01011112222");
    persistAndFlush(user);
    clear();

    // when
    User foundUser = repository.findWithRolesById(user.getId()).orElseThrow();

    // then
    assertUserFields(foundUser, true);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("다중 역할 포함 사용자 조회: USER와 PRODUCT_MANAGER를 모두 조회한다")
  void findWithRolesById_returnsAllUserRoles() {
    // given
    User user = User.createUser(UUID.randomUUID(), "tester", "01011112222");
    user.updateRoles(Set.of(UserRole.USER, UserRole.PRODUCT_MANAGER));
    persistAndFlush(user);
    clear();

    // when
    User foundUser = repository.findWithRolesById(user.getId()).orElseThrow();

    // then
    assertUserFields(foundUser, true, Set.of(UserRole.USER, UserRole.PRODUCT_MANAGER));
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("사용자 삭제: User를 삭제하면 물리 삭제 대신 비활성 상태로 저장한다")
  void delete_disablesUserWithoutPhysicalDeletion() {
    // given
    User user = User.createUser(UUID.randomUUID(), "tester", "01011112222");
    persistAndFlush(user);
    clear();

    // when
    repository.delete(user);
    flushAndClear();

    // then
    Optional<User> disabledUser = repository.findById(user.getId());
    User persistedUser = disabledUser.orElseThrow();
    assertUserScalarFields(persistedUser, false);
    assertUserFieldsLoaded(persistedUser);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("전화번호 존재 여부 조회: 저장된 전화번호를 찾으면 true를 반환한다")
  void existsByPhoneNumber_returnsTrueWhenPhoneNumberExists() {
    // given
    User user = User.createUser(UUID.randomUUID(), "tester", "01011112222");
    persistAndFlush(user);
    clear();

    // when
    boolean exists = repository.existsByPhoneNumber(user.getPhoneNumber());
    ensureQueryCount(1);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("전화번호 존재 여부 조회: 저장되지 않은 전화번호를 찾으면 false를 반환한다")
  void existsByPhoneNumber_returnsFalseWhenPhoneNumberDoesNotExist() {
    // given
    persistAndFlush(User.createUser(UUID.randomUUID(), "tester", "01011112222"));
    clear();

    // when
    boolean exists = repository.existsByPhoneNumber("01033334444");
    ensureQueryCount(1);

    // then
    assertThat(exists).isFalse();
  }

  private void assertUserFields(User user, boolean expectedEnabled) {
    assertUserFields(user, expectedEnabled, Set.of(UserRole.USER));
  }

  private void assertUserFields(User user, boolean expectedEnabled,
      Set<UserRole> expectedRoles) {
    assertUserScalarFields(user, expectedEnabled);
    assertThat(user.getRoles()).containsExactlyInAnyOrderElementsOf(expectedRoles);
  }

  private void assertUserScalarFields(User user, boolean expectedEnabled) {
    assertThat(user)
        .extracting(User::getId, User::getName, User::getPhoneNumber,
            User::getPointBalance, User::isEnabled)
        .containsExactly(user.getId(), "tester", "01011112222", 0, expectedEnabled);
    assertThat(user.getId()).isNotNull();
    assertThat(user.getName()).isNotNull();
    assertThat(user.getPhoneNumber()).isNotNull();
    assertThat(user.getPointBalance()).isGreaterThanOrEqualTo(0);
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getUpdatedAt()).isNotNull();
  }

  private void assertUserFieldsLoaded(User user) {
    assertThat(user.getId()).isNotNull();
    assertThat(user.getName()).isNotNull();
    assertThat(user.getPhoneNumber()).isNotNull();
    assertThat(user.getRoles()).isNotNull();
    assertThat(user.getPointBalance()).isGreaterThanOrEqualTo(0);
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getUpdatedAt()).isNotNull();
  }
}
