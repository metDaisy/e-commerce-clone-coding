package io.github.metdaisy.amaazon.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User 도메인 테스트")
class UserTest {

  @Test
  @DisplayName("사용자 생성 시 USER 역할을 기본으로 가진다")
  void createUser_hasUserRole() {
    User user = User.createUser(UUID.randomUUID(), "tester", "01012345678");

    assertThat(user.getRoles()).containsExactly(UserRole.USER);
  }

  @Test
  @DisplayName("사용자 역할을 여러 개 가질 수 있다")
  void updateRoles_supportsMultipleRoles() {
    User user = User.createUser(UUID.randomUUID(), "tester", "01012345678");

    user.updateRoles(Set.of(UserRole.USER, UserRole.PRODUCT_MANAGER));

    assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.USER, UserRole.PRODUCT_MANAGER);
  }

  @Test
  @DisplayName("사용자 역할에는 USER가 반드시 포함되어야 한다")
  void updateRoles_requiresUserRole() {
    User user = User.createUser(UUID.randomUUID(), "tester", "01012345678");

    assertThatThrownBy(() -> user.updateRoles(Set.of(UserRole.PRODUCT_MANAGER)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User roles must contain USER");
  }

  @Test
  @DisplayName("사용자 역할 집합은 외부에서 변경할 수 없다")
  void roles_areUnmodifiable() {
    User user = User.createUser(UUID.randomUUID(), "tester", "01012345678");

    assertThatThrownBy(() -> user.getRoles().add(UserRole.ADMIN))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
