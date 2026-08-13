# ADR-0007: Category 애플리케이션 캐시와 Tag 2차 캐시

- 상태: 승인됨
- 날짜: 2026-08-13
- 결정자: 사용자, Codex
- 대체하는 ADR: 없음
- 대체된 ADR: 없음

## 배경

Category는 `GET /api/v1/categories`를 통해 전체 트리로 제공되며 변경이
드물다. 애플리케이션에는 단일 Category 조회 경로가 없다. Tag는 현재
불변이며 수정 및 삭제 동작이 없다.

Category에 Hibernate 2차 캐시를 사용하면 애플리케이션이 하나의 트리형
조회 결과를 사용하더라도 엔티티 및 컬렉션 영역을 각각 캐시하게 된다.
Hibernate Query Cache를 사용하려면 해당 조회를 위해 timestamp 영역과
query-result 영역도 필요하다.

## 결정

- 변환된 카테고리 트리를 Spring Cache의 `categories` 이름으로 캐시한다.
- 카테고리 목록 캐시 항목은 하나만 유지하고, 마지막 접근 후 1시간이
  지나면 만료한다.
- Category 생성 또는 수정 명령이 성공하면 `categories`의 모든 항목을
  제거한다.
- Category의 Hibernate 캐시 애노테이션과 Hibernate Query Cache 사용을
  제거한다.
- 기존 `READ_ONLY` 영역을 사용하여 Tag의 Hibernate 2차 캐시는 유지한다.
- Tag 변경 기능이 현재 지원되지 않으므로 Tag JCache 영역 설정을
  유지한다.

## 결과

- Category 캐시에는 관리되는 JPA 엔티티나 Hibernate 컬렉션 항목이 아니라
  DTO가 저장된다.
- Category 쓰기 작업이 발생하면 다음 카테고리 목록 요청이 데이터베이스에서
  트리를 다시 읽고 캐시를 재생성한다.
- Hibernate Query Cache 영역인
  `default-update-timestamps-region`과 `default-query-results-region`은
  Category에 더 이상 필요하지 않다.
- Tag를 변경할 수 있게 되면 변경 기능을 활성화하기 전에 `READ_ONLY`
  전략과 캐시 무효화 정책을 변경해야 한다.

## 근거

- [카탈로그 캐시 정책](../caching.md)
- [Category 조회 서비스](../../src/main/java/io/github/metdaisy/amaazon/catalog/application/service/category/CategoryQueryService.java)
- [Category 명령 서비스](../../src/main/java/io/github/metdaisy/amaazon/catalog/application/service/category/CategoryCommandService.java)
