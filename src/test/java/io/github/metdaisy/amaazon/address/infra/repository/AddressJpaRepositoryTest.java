package io.github.metdaisy.amaazon.address.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Address JPA 저장소 슬라이스 테스트")
class AddressJpaRepositoryTest extends BaseRepositoryTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Autowired
  private AddressJpaRepository repository;

  @Test
  @DisplayName("주소 저장: Address를 저장한 뒤 같은 식별자로 조회할 수 있다")
  void save_persistsAddress() {
    // given
    Address address = address(false);

    // when
    Address savedAddress = repository.save(address);
    flushAndClear();
    Address foundAddress = repository.findById(address.getId()).orElseThrow();

    // then
    assertThat(savedAddress.getId()).isEqualTo(address.getId());
    assertAddressFields(foundAddress, false);
    ensureQueryCount(1);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidAddresses")
  @DisplayName("주소 저장 실패: 필수 값이 없거나 길이를 초과하면 저장하지 않는다")
  void save_rejectsInvalidAddress(String caseName, Address invalidAddress,
      String expectedField, String expectedConstraint) {
    // when
    ConstraintViolationException exception = catchThrowableOfType(() -> {
      repository.save(invalidAddress);
      em.flush();
    }, ConstraintViolationException.class);

    // then
    assertThat(exception.getConstraintViolations()).singleElement().satisfies(violation -> {
      assertThat(violation.getPropertyPath().toString()).isEqualTo(expectedField);
      assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()
          .getSimpleName()).isEqualTo(expectedConstraint);
    });
  }

  @Test
  @DisplayName("주소 조회: User별 주소를 기본 배송지와 생성 시각 순으로 조회한다")
  void findByUserId_returnsSortedAddresses() {
    // given
    persistAndFlush(address(false));
    persistAndFlush(address(true));
    persistAndFlush(address(false));
    clear();

    // when
    List<Address> result = repository.findByUserId(USER_ID);

    // then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).isPrimary()).isTrue();
    result.forEach(this::assertAddressLoaded);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("주소 삭제: 저장된 Address를 물리적으로 삭제한다")
  void delete_removesAddress() {
    // given
    Address address = persistAndFlush(address(false));
    clear();

    // when
    repository.delete(address);
    flushAndClear();

    // then
    assertThat(repository.findById(address.getId())).isEmpty();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("기본 배송지 해제: User의 기본 배송지를 한 번에 해제한다")
  void clearPrimaryByUserId_updatesPrimaryAddress() {
    // given
    persistAndFlush(address(true));
    clear();

    // when
    int updatedCount = repository.clearPrimaryByUserId(USER_ID);
    flushAndClear();

    // then
    List<Address> updatedAddresses = repository.findByUserId(USER_ID);
    assertThat(updatedCount).isEqualTo(1);
    assertThat(updatedAddresses).hasSize(1);
    assertThat(updatedAddresses.get(0).isPrimary()).isFalse();
    assertAddressLoaded(updatedAddresses.get(0));
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("기본 배송지 지정: 기존 기본 배송지를 해제하고 대상 주소를 기본값으로 변경한다")
  void makePrimaryByIdAndUserId_updatesOnlyTargetAddress() {
    // given
    Address oldPrimary = persistAndFlush(address(true));
    Address target = persistAndFlush(
        Address.create(USER_ID, "target", "01012345678", "06237", "부산", false));
    clear();

    // when
    int clearedCount = repository.clearPrimaryByUserId(USER_ID);
    int updatedCount = repository.makePrimaryByIdAndUserId(target.getId(), USER_ID);
    flushAndClear();

    // then
    List<Address> addresses = repository.findByUserId(USER_ID);
    assertThat(clearedCount).isEqualTo(1);
    assertThat(updatedCount).isEqualTo(1);
    assertThat(addresses).extracting(Address::getId)
        .containsExactly(target.getId(), oldPrimary.getId());
    assertThat(addresses).extracting(Address::isPrimary)
        .containsExactly(true, false);
    addresses.forEach(this::assertAddressLoaded);
    ensureQueryCount(1);
  }

  private static List<Arguments> invalidAddresses() {
    return List.of(
        Arguments.of("소유 User 누락",
            Address.create(null, "tester", "01012345678", "06236", "서울", false),
            "userId", "NotNull"),
        Arguments.of("수령인 이름 누락",
            Address.create(USER_ID, null, "01012345678", "06236", "서울", false),
            "recipientName", "NotNull"),
        Arguments.of("수령인 이름 길이 초과",
            Address.create(USER_ID, "a".repeat(101), "01012345678", "06236", "서울", false),
            "recipientName", "Size"),
        Arguments.of("수령인 연락처 누락",
            Address.create(USER_ID, "tester", null, "06236", "서울", false),
            "recipientPhone", "NotNull"),
        Arguments.of("수령인 연락처 길이 초과",
            Address.create(USER_ID, "tester", "1".repeat(21), "06236", "서울", false),
            "recipientPhone", "Size"),
        Arguments.of("우편번호 누락",
            Address.create(USER_ID, "tester", "01012345678", null, "서울", false),
            "postalCode", "NotNull"),
        Arguments.of("우편번호 길이 초과",
            Address.create(USER_ID, "tester", "01012345678", "1".repeat(21), "서울", false),
            "postalCode", "Size"),
        Arguments.of("주소 본문 누락",
            Address.create(USER_ID, "tester", "01012345678", "06236", null, false),
            "addressLine", "NotNull"),
        Arguments.of("주소 본문 길이 초과",
            Address.create(USER_ID, "tester", "01012345678", "06236", "a".repeat(256), false),
            "addressLine", "Size"));
  }

  private static Address address(boolean isPrimary) {
    return Address.create(USER_ID, "tester", "01012345678", "06236", "서울특별시 강남구", isPrimary);
  }

  private void assertAddressFields(Address address, boolean expectedPrimary) {
    assertThat(address)
        .extracting(Address::getId, Address::getUserId, Address::getRecipientName,
            Address::getRecipientPhone, Address::getPostalCode, Address::getAddressLine,
            Address::isPrimary)
        .containsExactly(address.getId(), USER_ID, "tester", "01012345678", "06236",
            "서울특별시 강남구", expectedPrimary);
    assertAddressLoaded(address);
  }

  private void assertAddressLoaded(Address address) {
    assertThat(address.getId()).isNotNull();
    assertThat(address.getUserId()).isNotNull();
    assertThat(address.getRecipientName()).isNotNull();
    assertThat(address.getRecipientPhone()).isNotNull();
    assertThat(address.getPostalCode()).isNotNull();
    assertThat(address.getAddressLine()).isNotNull();
    assertThat(address.getCreatedAt()).isNotNull();
    assertThat(address.getUpdatedAt()).isNotNull();
  }
}
