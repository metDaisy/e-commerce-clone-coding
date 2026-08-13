# 카탈로그 캐시 정책

## Category

`GET /api/v1/categories`는 전체 카테고리 트리를 반환하므로 개별 `Category`
엔티티를 캐시하지 않는다. 애플리케이션은 변환된
`List<CategoryResponse>`를 Spring Cache 이름 `categories`로 캐시한다.

캐시 항목은 하나만 유지하며 마지막 접근 후 1시간이 지나면 만료된다.
Category 생성과 수정 명령이 성공하면 모든 캐시 항목을 제거한다. 이후
카테고리 목록 조회 요청이 데이터베이스에서 트리를 다시 읽고 캐시를
재생성한다.

따라서 Category는 Hibernate 2차 캐시나 Hibernate Query Cache를 사용하지
않는다. 트리를 조회할 때 JPA `children` 관계는 사용하지만, 해당 관계는
Hibernate 캐시 영역이 아니다.

## Tag

현재 `Tag`는 불변 객체로 취급한다. 현재 애플리케이션에는 Tag 수정 및
삭제 동작이 없다. 따라서 `Tag`라는 이름의 `READ_ONLY` 엔티티 영역으로
Hibernate 2차 캐시에 유지한다.

JCache provider 설정은 `Tag` 영역을 마지막 접근 후 1시간이 지나면
만료하도록 유지한다. 이후 Tag 변경 기능을 도입한다면 `READ_ONLY` 전략과
캐시 무효화 정책을 함께 재검토해야 한다.

## 캐시 전략을 다르게 사용하는 이유

Category 조회는 하나의 집계 형태 트리를 반환하므로, Category가 하나라도
변경되면 애플리케이션 캐시 항목 하나를 무효화하는 방식이 명확하다.
반면 Tag 조회는 엔티티 단위 참조이고 현재 Tag가 불변이므로 Hibernate의
읽기 전용 2차 캐시가 적절하다.
