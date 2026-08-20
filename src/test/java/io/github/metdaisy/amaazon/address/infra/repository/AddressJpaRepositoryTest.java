package io.github.metdaisy.amaazon.address.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    em.flush();
    ensureQueryCount(1);
    clear();
    Address foundAddress = repository.findById(address.getId()).orElseThrow();

    // then
    assertThat(savedAddress.getId()).isEqualTo(address.getId());
    assertAddressFields(foundAddress, false);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("최근 사용 시각 저장: Address의 lastUsedAt을 저장하고 조회한다")
  void save_persistsLastUsedAt() {
    // given
    Instant lastUsedAt = Instant.parse("2026-08-17T12:00:00Z");
    Address address = address(false);
    address.markUsed(lastUsedAt);

    // when
    repository.save(address);
    em.flush();
    ensureQueryCount(1);
    clear();
    Address foundAddress = repository.findById(address.getId()).orElseThrow();

    // then
    assertThat(foundAddress.getLastUsedAt()).isEqualTo(lastUsedAt);
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
    ensureQueryCount(0);

    // then
    assertThat(exception.getConstraintViolations()).singleElement().satisfies(violation -> {
      assertThat(violation.getPropertyPath().toString()).isEqualTo(expectedField);
      assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()
          .getSimpleName()).isEqualTo(expectedConstraint);
    });
  }

  @Test
  @DisplayName("주소 조회: User별 주소를 기본 배송지와 최근 사용 시각 순으로 조회한다")
  void findByUserId_returnsSortedAddresses() {
    // given
    persistAndFlush(address(false));
    persistAndFlush(Address.create(USER_ID, "기본", "primary", "01098765432", "06237", "부산",
        true));
    persistAndFlush(Address.create(USER_ID, "추가", "other", "01011112222", "06238", "대구",
        false));
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
  @DisplayName("주소 페이지 조회: User별 주소를 페이지 단위로 반환한다")
  void findByUserId_returnsPage() {
    // given
    persistAndFlush(address(false));
    persistAndFlush(Address.create(USER_ID, "회사", "office", "01098765432", "06237", "부산",
        false));
    persistAndFlush(Address.create(USER_ID, "추가", "other", "01011112222", "06238", "대구",
        false));
    clear();

    // when
    Page<Address> firstPage = repository.findByUserId(USER_ID, PageRequest.of(0, 2));

    // then
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.getTotalElements()).isEqualTo(3);
    firstPage.forEach(this::assertAddressLoaded);
    ensureQueryCount(2);

    clear();
    Page<Address> secondPage = repository.findByUserId(USER_ID, PageRequest.of(1, 2));
    assertThat(secondPage.getContent()).hasSize(1);
    assertThat(secondPage.getTotalElements()).isEqualTo(3);
    secondPage.forEach(this::assertAddressLoaded);
    ensureQueryCount(1);

    assertThat(secondPage.getContent()).extracting(Address::getId)
        .doesNotContainAnyElementsOf(firstPage.getContent().stream()
            .map(Address::getId)
            .toList());
  }

  @Test
  @DisplayName("주소 페이지 결과 조회: 공통 PageResult로 변환하고 쿼리 수를 검증한다")
  void findPageByUserId_returnsPageResult() {
    // given
    persistAndFlush(address(false));
    persistAndFlush(Address.create(USER_ID, "회사", "office", "01098765432", "06237", "부산",
        false));
    persistAndFlush(Address.create(USER_ID, "추가", "other", "01011112222", "06238", "대구",
        false));
    clear();

    // when
    PageResult<Address> firstPage = repository.findPageByUserId(USER_ID, new PageQuery(0, 2));

    // then
    assertThat(firstPage.content()).hasSize(2);
    assertThat(firstPage.totalElements()).isEqualTo(3);
    firstPage.content().forEach(this::assertAddressLoaded);
    ensureQueryCount(2);

    clear();
    PageResult<Address> secondPage = repository.findPageByUserId(USER_ID, new PageQuery(1, 2));
    assertThat(secondPage.content()).hasSize(1);
    assertThat(secondPage.totalElements()).isEqualTo(3);
    secondPage.content().forEach(this::assertAddressLoaded);
    ensureQueryCount(1);

    assertThat(secondPage.content()).extracting(Address::getId)
        .doesNotContainAnyElementsOf(firstPage.content().stream()
            .map(Address::getId)
            .toList());
  }

  @Test
  @DisplayName("주소 중복 여부 조회: User의 동일 주소가 존재하면 true를 반환한다")
  void existsByUserIdAndPostalCodeAndAddressLine_returnsTrueWhenAddressExists() {
    // given
    persistAndFlush(address(false));
    clear();

    // when
    boolean exists = repository.existsByUserIdAndPostalCodeAndAddressLine(
        USER_ID, "06236", "서울특별시 강남구");
    ensureQueryCount(1);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("주소 중복 여부 조회: 다른 User의 주소는 중복으로 판단하지 않는다")
  void existsByUserIdAndPostalCodeAndAddressLine_returnsFalseForUnknownAddress() {
    // when
    boolean exists = repository.existsByUserIdAndPostalCodeAndAddressLine(
        USER_ID, "06236", "서울특별시 강남구");
    ensureQueryCount(1);

    // then
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("주소 수정 중복 여부 조회: 자기 자신을 제외한 동일 주소가 있으면 true를 반환한다")
  void existsByUserIdAndPostalCodeAndAddressLineAndIdNot_returnsTrueForAnotherAddress() {
    // given
    Address existing = persistAndFlush(address(false));
    clear();

    // when
    boolean exists = repository.existsByUserIdAndPostalCodeAndAddressLineAndIdNot(
        USER_ID, existing.getPostalCode(), existing.getAddressLine(), UUID.randomUUID());
    ensureQueryCount(1);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("주소 소유권 조회: 주소가 User에 속하면 true를 반환한다")
  void existsByIdAndUserId_returnsTrueWhenAddressBelongsToUser() {
    // given
    Address address = persistAndFlush(address(false));
    clear();

    // when
    boolean exists = repository.existsByIdAndUserId(address.getId(), USER_ID);
    ensureQueryCount(1);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("주소 소유권 조회: 다른 User의 주소이면 false를 반환한다")
  void existsByIdAndUserId_returnsFalseForAnotherUser() {
    // given
    Address address = persistAndFlush(address(false));
    clear();

    // when
    boolean exists = repository.existsByIdAndUserId(address.getId(), UUID.randomUUID());
    ensureQueryCount(1);

    // then
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("주소 저장 실패: 동일 User의 동일 주소는 데이터베이스에서 중복 저장할 수 없다")
  void save_rejectsDuplicatedAddress() {
    // given
    persistAndFlush(address(false));
    Address duplicated = Address.create(USER_ID, "다른 별칭", "other", "01098765432", "06236",
        "서울특별시 강남구", false);
    clear();

    // when & then
    assertThatThrownBy(() -> {
      repository.save(duplicated);
      em.flush();
    }).hasMessageContaining("uq_addresses_user_postal_address")
        .satisfies(exception -> assertThat(exception.getClass().getName())
            .isEqualTo("org.hibernate.exception.ConstraintViolationException"));
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
    em.flush();

    // then
    ensureQueryCount(2);
    clear();
    assertThat(repository.findById(address.getId())).isEmpty();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("주소 일괄 삭제: 유저의 유일한 주소를 삭제한다")
  void deleteAndUpdatePrimary_removesOnlyAddress() {
    // given
    Address address = persistAndFlush(address(true));
    clear();

    // when
    repository.deleteAndUpdatePrimary(USER_ID, address.getId());
    em.flush();

    // then
    ensureQueryCount(3);
    clear();
    assertThat(repository.findById(address.getId())).isEmpty();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("주소 일괄 삭제: 두 주소 중 첫 번째를 삭제하고 두 번째를 기본 배송지로 변경한다")
  void deleteAndUpdatePrimary_promotesSecondAddress() {
    // given
    Address target = persistAndFlush(address(true));
    Address replacement = persistAndFlush(
        Address.create(USER_ID, "회사", "office", "01098765432", "06237", "부산광역시",
            false));
    clear();

    // when
    repository.deleteAndUpdatePrimary(USER_ID, target.getId());
    em.flush();

    // then
    ensureQueryCount(4);
    clear();
    List<Address> addresses = repository.findByUserId(USER_ID);
    assertThat(addresses).hasSize(1);
    assertThat(addresses.get(0).getId()).isEqualTo(replacement.getId());
    assertThat(addresses.get(0).isPrimary()).isTrue();
    addresses.forEach(this::assertAddressLoaded);
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("주소 일괄 삭제: 기본 배송지가 아닌 주소를 삭제해도 기존 기본 배송지를 유지한다")
  void deleteAndUpdatePrimary_keepsExistingPrimaryAddress() {
    // given
    Address replacement = persistAndFlush(address(true));
    Address target = persistAndFlush(
        Address.create(USER_ID, "회사", "office", "01098765432", "06237", "부산광역시",
            false));
    clear();

    // when
    repository.deleteAndUpdatePrimary(USER_ID, target.getId());
    em.flush();

    // then
    ensureQueryCount(3);
    clear();
    List<Address> addresses = repository.findByUserId(USER_ID);
    assertThat(addresses).hasSize(1);
    assertThat(addresses.get(0).getId()).isEqualTo(replacement.getId());
    assertThat(addresses.get(0).isPrimary()).isTrue();
    addresses.forEach(this::assertAddressLoaded);
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
    em.flush();

    // then
    ensureQueryCount(1);
    clear();
    List<Address> updatedAddresses = repository.findByUserId(USER_ID);
    assertThat(updatedCount).isEqualTo(1);
    assertThat(updatedAddresses).hasSize(1);
    assertThat(updatedAddresses.get(0).isPrimary()).isFalse();
    assertAddressLoaded(updatedAddresses.get(0));
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("기본 배송지 지정: 기존 기본 배송지를 해제하고 대상 주소를 기본값으로 변경한다")
  void makePrimary_updatesTargetAndClearsPreviousPrimary() {
    // given
    Address oldPrimary = persistAndFlush(address(true));
    Address target = persistAndFlush(
        Address.create(USER_ID, "회사", "target", "01012345678", "06237", "부산", false));
    clear();

    // when
    Address updatedAddress = repository.makePrimary(USER_ID, target.getId());
    em.flush();

    // then
    ensureQueryCount(4);
    clear();
    List<Address> addresses = repository.findByUserId(USER_ID);
    assertThat(updatedAddress.getId()).isEqualTo(target.getId());
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
            Address.create(null, "집", "tester", "01012345678", "06236", "서울", false),
            "userId", "NotNull"),
        Arguments.of("주소 별칭 누락",
            Address.create(USER_ID, null, "tester", "01012345678", "06236", "서울", false),
            "alias", "NotNull"),
        Arguments.of("주소 별칭 길이 초과",
            Address.create(USER_ID, "a".repeat(101), "tester", "01012345678", "06236", "서울",
                false),
            "alias", "Size"),
        Arguments.of("수령인 이름 누락",
            Address.create(USER_ID, "집", null, "01012345678", "06236", "서울", false),
            "recipientName", "NotNull"),
        Arguments.of("수령인 이름 길이 초과",
            Address.create(USER_ID, "집", "a".repeat(101), "01012345678", "06236", "서울", false),
            "recipientName", "Size"),
        Arguments.of("수령인 연락처 누락",
            Address.create(USER_ID, "집", "tester", null, "06236", "서울", false),
            "recipientPhone", "NotNull"),
        Arguments.of("수령인 연락처 길이 초과",
            Address.create(USER_ID, "집", "tester", "1".repeat(21), "06236", "서울", false),
            "recipientPhone", "Size"),
        Arguments.of("우편번호 누락",
            Address.create(USER_ID, "집", "tester", "01012345678", null, "서울", false),
            "postalCode", "NotNull"),
        Arguments.of("우편번호 길이 초과",
            Address.create(USER_ID, "집", "tester", "01012345678", "1".repeat(21), "서울", false),
            "postalCode", "Size"),
        Arguments.of("주소 본문 누락",
            Address.create(USER_ID, "집", "tester", "01012345678", "06236", null, false),
            "addressLine", "NotNull"),
        Arguments.of("주소 본문 길이 초과",
            Address.create(USER_ID, "집", "tester", "01012345678", "06236", "a".repeat(256), false),
            "addressLine", "Size"));
  }

  private static Address address(boolean isPrimary) {
    return Address.create(USER_ID, "집", "tester", "01012345678", "06236", "서울특별시 강남구",
        isPrimary);
  }

  private void assertAddressFields(Address address, boolean expectedPrimary) {
    assertThat(address)
        .extracting(Address::getId, Address::getUserId, Address::getAlias, Address::getRecipientName,
            Address::getRecipientPhone, Address::getPostalCode, Address::getAddressLine,
            Address::isPrimary)
        .containsExactly(address.getId(), USER_ID, "집", "tester", "01012345678", "06236",
            "서울특별시 강남구", expectedPrimary);
    assertAddressLoaded(address);
  }

  private void assertAddressLoaded(Address address) {
    assertThat(address.getId()).isNotNull();
    assertThat(address.getUserId()).isNotNull();
    assertThat(address.getAlias()).isNotNull();
    assertThat(address.getRecipientName()).isNotNull();
    assertThat(address.getRecipientPhone()).isNotNull();
    assertThat(address.getPostalCode()).isNotNull();
    assertThat(address.getAddressLine()).isNotNull();
    address.getLastUsedAt();
    assertThat(address.getCreatedAt()).isNotNull();
    assertThat(address.getUpdatedAt()).isNotNull();
  }
}
