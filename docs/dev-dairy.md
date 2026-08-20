# Development Diary

프로젝트를 진행하며 사용자가 직접 남기는 시간순 개발 일지다. 당시의 고민, 가설,
실험, 판단 과정을 보존하며 확정된 요구사항이나 현재 상태의 원본으로 사용하지 않는다.

- 에이전트는 현재 작업과 관련된 날짜·주제만 선택해서 읽는다.
- 에이전트는 사용자의 명시적 요청 없이 이 문서를 수정, 요약, 재배열하지 않는다.
- 확정된 구조적 결정은 `docs/adr/`, 현재 구현 결과는 `current-state.md`에 별도로 반영한다.
- 새 기록은 기존 문체를 유지하며 날짜 제목 아래에 추가한다.

---

<details>

<summary><h2>2026-07-31</h2></summary>

현재 회원가입 interface의 진입점과 전체 흐름 조정은 `user` 모듈이 담당한다.  
회원가입 요청으로 이름, 이메일, 평문 비밀번호, 연락처, 주소를 전달받으며, 이름·연락처·주소는 `users`에 저장하고 로그인 이메일과 비밀번호 해시는
`user_credentials`에 저장한다.  
이메일과 비밀번호는 로컬 로그인 인증수단으로 사용된다.  
소셜 로그인 사용자는 `UserCredential`을 생성하지 않으므로, 도메인상 `User`를 기준으로 `UserCredential`과의 관계는 1:0..1이다.  
`user` 모듈은 사용자 프로필, 역할, 포인트를 관리한다.  
`auth` 모듈은 로컬·소셜 인증수단의 등록, 검증, 변경을 관리한다.  
비밀번호 정책 검사, 해시, 일치 여부 확인도 `auth` 모듈 책임에 포함된다.  
그러나 현재 비밀번호 유효성 검사가 `user` 모듈에 존재하며, 평문 비밀번호가 `UserCreateRequest`, `UserService`,
`UserCreatedEvent`, `AuthEventHandler`를 거쳐 `AuthService`까지 전달된다.  
이러한 구조가 `User`와 `UserCredential`의 분리 자체를 무효화하지는 않는다.  
두 엔티티와 테이블을 분리함으로써 저장 구조, 조회 범위, 변경 주기, 접근 책임을 분리하는 효과는 여전히 존재한다.  
다만 평문 비밀번호를 다루는 객체와 모듈이 증가하여 평문 비밀번호의 생존 범위와 잠재적 노출 지점이 넓어진다.  
또한 `user` 모듈이 비밀번호 정책과 `credential` 생성 흐름을 알아야 하므로 인증 책임이 `user` 모듈로 누수된다.  
결과적으로 민감정보 분리 효과와 module locality가 약화된다.  
특히 현재 `UserCreatedEvent`는 사용자 생성이 완료됐다는 사실을 알리는 이벤트라기보다, `auth` 모듈에 `credential` 생성을 요청하는 명령처럼 사용되고
있다.  
회원가입 정합성도 동기 이벤트 처리와 동일 트랜잭션 전파라는 구현 세부사항에 의존한다.  
따라서 회원가입 workflow를 `auth` 모듈에 집중한다. `auth` 모듈이 회원가입 요청을 받고 비밀번호 정책을 검사한 뒤 비밀번호를 해시한다.  
이후 `user` 모듈의 프로필 생성 seam에는 이름, 연락처, 주소만 전달하고, 반환된 사용자 ID로 `UserCredential`을 생성한다.  
프로필과 `credential` 생성은 하나의 트랜잭션에서 처리하여 둘 중 하나라도 실패하면 모두 롤백한다.  
가입 완료 후 이벤트가 필요하다면 평문 비밀번호를 제외하고 사용자 ID 같은 비민감 정보만 포함한다.

</details>

---

<details>

<summary><h2>2026-08-03</h2></summary>

Gemini, codex, local llm agent 3개의 agent 를 사용하고 있다.  
모든 agent 에게 mcp 를 각각 추가하였다.  
동일한 mcp 프로세스가 3개씩 실행되고 있어 이를 해결하기 위해 `mcp jungle` 을 사용했다.  
이것은 여러 agent 가 하나의 mcp 서버를 사용할 수 있도록 할 수 있다.  
또한 각 agent 에게 적용한 rule 의 차이가 있어 이를 통일했다.  
매 새로운 대화를 할 때 마다 현 프로젝트에 대한 상황을 매번 전달해야 했다.  
이것들을 정리해 `AGENTS.md` 에 작성하였다.

</details>

<details>

<summary><h2>2026-08-04</h2></summary>

Continue에 연결한 KAT-Coder와 Qwen3.6 계열 local agent가 도구명을 자주 오타 내고, 같은 오타를 반복 호출하는 문제가 있었다.  
처음에는 모델의 tool calling 능력이나 `qwen3-coder parser` 문제로 추정했다.  
원인을 좁히기 위해 llama-server 시작 로그의 chat template, `chat_format: peg-native`, `--log-prompts-dir`로 저장한
프롬프트, Continue Console의 tool call 기록을 차례로 확인했다.  
llama-server는 `Qwen3-Coder` 템플릿과 `peg-native` parser를 사용하고 있었지만, 
Continue가 처음에는 native `tools` 배열 대신 `<tool_use_instructions>`와 `TOOL_NAME` 목록을 system message에 넣고 있었다.  
이 방식에서는 모델이 도구명을 자유 텍스트로 생성하므로 llama.cpp의 도구명 문법 제한을 적용할 수 없었다.  
KAT뿐 아니라 Qwen3.6에서도 같은 문제가 발생한 것은 모델보다 Continue의 도구 전달 방식이 공통 원인이었기 때문이다.  
Continue의 system-message tool 경로를 native tool calling 경로로 변경하고, OpenAI 호환 `/v1/chat/completions` 요청에 `tools` 배열이 전달되는지 확인했다.  
프롬프트에는 `<tools>`와 Qwen3-Coder 형식의 `<tool_call>`이 나타났고, non-stream 응답은 `message.tool_calls` 안에 정확한 도구명과 JSON 인자를 반환했다.  
streaming 응답도 직접 확인했다.  
`arguments`가 `{"`, `"path":"`, 경로 문자열처럼 여러 chunk로 나뉘는 것은 정상적인 streaming delta였다.  
Continue Console에서 각 chunk가 별도의 `Tool call`처럼 표시되었지만, 실제 `filesystem__read_text_file` 실행은 한 번뿐이었다.  
따라서 반복 호출로 보였던 일부 로그는 Continue Console의 streaming 표시였고, 현재 local agent가 MCP 도구를 native 방식으로 정상 호출·실행하는 것을 확인했다.

</details>

<details>

<summary><h2>2026-08-05</h2></summary>

`AuthenticationFailureHandler`가 다룰 수 있는 `AuthenticationException` 들은 인증 과정(`AuthenticationProvider`, `UserDetailsService`) 에서 발생하는 예외들이다.  
`BadCredentialsException`: 비밀번호 불일치. spring security 기본 설정(`hideUserNotFoundExceptions = true`)에 의해 `UsernameNotFoundException`이 발생하면 `BadCredentialsException`으로 다시 생성된다.  
`DisabledException`, `LockedException`, `AccountExpiredException`, `CredentialsExpiredException`, `AuthenticationServiceException`, `SessionAuthenticationException`  

`UserDetailsService`의 `loadUserByUsername` 에는 `UsernameNotFoundException`이 던져지도록 되어 있다.  
만약 `AuthenticationException` 을 상속하지 않은 custom exception 을 던진다면 `InternalAuthenticationServiceException` 으로 wrapping 된다.  
`UsernameNotFoundException` 을 던진다면 `BadCredentialsException` 이 발생하여 email 찾을 수 없음 혹은 비밀번호 불일치를 구분할 수 없다.  
비밀번호 불일치 횟수를 기록해야 하므로 이를 구분해야 한다.  
`AuthenticationException` 을 상속한 custom exception 을 던지도록 수정할 예정이다.  
`AuthenticationEntryPoint` 는 인증이 필요한 리소스에 미인증 상태로 접근할 때 발생하는 예외를 다룬다.  

비밀번호 불일치 및 계정 잠금 정책  
실패 횟수를 기록한다. `maxAttempt`에 도달하면 `lockedDuration` 동안 잠금 상태가 된다.  
잠금 상태에선 실패 횟수, 잠금 기간 변화가 없다. 또한 로그인 시도 자체가 차단된다.  
로그인 성공해도 실패 횟수는 유지된다. 다중 접속을 허용하기 때문이다.  
잠금 기간이 끝나면 리셋이 된다.  
현재 외부 인프라(redis) 도입은 아직 고려하지 않고 있다.  
따라서 in-memory cache + jpa 로 구현할 예정이다.  

</details>

<details>

<summary><h2>2026-08-06</h2></summary>

에이전트에게 컨텍스트를 너무 많게 혹은 적게 전달하여 요구사항을 제대로 수행하지 못한 경우가 있다.  
특히 대화가 길어지면 요구사항을 잊어버리는 현상이 생긴다.  
요구사항과 관련된 핵심 컨텍스트를 정제해서 넘긴다면? 이라는 생각이 들었다.  
그러다가 `컨텍스트 엔지니어링`을 알게 되었다.  
LLM 은 높은 추론 능력을 갖게 되었지만 주어진 컨텍스트에 따라 성능이 달라진다.  
에이전트는 불필요하게 파일 전부를 읽는다.  
컨텍스트를 큐레이션할 수 없을까 라는 필요성을 느껴 여러 프레임워크를 살펴 보다가 `LlamaIndex`를 알게 되었다.  
`LlamaIndex` 는 수 많은 데이터에 대해 유저의 복잡한 질문에 맞는 결과를 찾아준다.  
즉 원하는 것을 잘 찾아준다고 해서 구현을 시작했다.  
한 번의 검색을 통해 얻은 컨텍스트로 나의 질문에 대한 대답을 생성하면 좋겠지만 아닌 경우는?  
간단한 질문에도 수 많은 검색을 하고 있어서 워크플로우를 최적화할 필요를 느꼈다.  
처음은 가벼운 마음으로 시작했지만 하면 할수록 고려할 것이 너무 많아 배보다 배꼽이 더 커져버렸다..

</details>

<details>

<summary><h2>2026-08-07</h2></summary>

jpa 를 공부하면서 1차 캐시라는 단어를 많이 접했다.  
이건 영속성 컨텍스트를 의미한다.  
jpa 는 규격(인터페이스)이며 실체는 springboot 에 내장된 hibernate 이다.  
1차 캐시는 사실 hibernate 1차 캐시이다.  
2차 캐시는 많이 접하지 않아 정확히 몰랐다.  
사용할 일이 없었고 spring cache 를 사용했기 때문이다.  
spring cache 는 메소드 반환값을, 2차 캐시는 엔티티를 저장한다.  
그래서 전자를 많이 사용했다.  
`Category` 엔티티는 meta data 로서 유저가 생성하는 데이터가 아니다.  
웹 어플 관리자가 생성 및 삭제를 한다.  
이 데이터는 많지 않고 수정을 하지 않는다.  
`Product` 를 조회하면 반드시 가져와야 한다.  
`Tag` 와 `Product` 는 N:M 관계이다.  
전자는 유저들에 의해 생성된다.  
`Product` 는 `Tag` 를 mappedBy 로 참조한다.  

이러한 특성 때문에 `Category`와 `Tag`는 2차 캐시(Second-Level Cache)를 적용하기에 아주 적합하다.  
거의 수정되지 않고 빈번하게 조회되는 데이터이므로, 캐싱을 통해 DB 접근 횟수를 획기적으로 줄일 수 있기 때문이다.  
또한 `Product`와 `Tag`는 N:M(다대다) 관계이므로 원래대로라면 `@ManyToMany`를 사용할 수 있다.  
하지만 실무적인 관점에서 추후 상품에 태그를 노출할 때 정렬 순서(`sortOrder`) 등 추가 컬럼이 중간 조인 테이블에 필요해질 수 있다.  
`@ManyToMany`가 자동 생성하는 조인 테이블에는 추가 컬럼을 매핑하기 어렵기 때문에, 향후 확장성을 고려하여 `ProductTag`라는 중간 엔티티 클래스를 직접 만들었다.  

이로 인해 `Product`와 `ProductTag` 간에 1:N 관계가 형성되었다.  
만약 `Product` 커서 조회(Cursor Pagination) 시 연관된 태그들을 한 번에 가져오기 위해 `fetch join`을 사용하면 데이터 뻥튀기(Cartesian Product) 문제가 발생한다.  
반대로 지연 로딩(Lazy Loading)으로 조회하면 N+1 쿼리 문제가 발생하는데, `product_tags` 에 `product_id` 를 조회해 연관된 `tag_id`들을 얻는 쿼리 1번, `Tag` 는 캐싱했으니 쿼리 발생하지 않으므로(캐싱되지 않은 `Tag`가 있다면 쿼리 발생) 최소 1번 발생한다.

</details>

<details>

<summary><h2>2026-08-12</h2></summary>

`CatalogProduct` 의 response dto 로 변환하면서 `updatedAt` 에 대한 고민이 생겼다.  
`@LastModifiedDate` annotation 을 붙이면 dirty checking 을 통해 update query 를 생성할 때 업데이트 된다.  
문제는 이 과정은 transaction 에서 일어나는데 만약 entity class -> response dto 로 변환할 때 updatedAt 은 최신화된 값이 아니다.  
dirty checking 은 transaction aop 에서 일어나기 때문이다.  
`CatalogProduct` 는 field 갯수가 많아 mapper 로 객체 생성 및 업데이트를 위임하고 있다.  
업데이트가 일어났는지 확인하는 건 적절하지 않은 것 같고 `flush()`를 하자니 dirty checking 을 2번하게 되서 적절하지 않아 보인다.  
일단 updatedAt 에 `@Setter` 를 추가해 업데이트 시 현재 시간으로 업데이트하도록 했다.  

</details>

<details>

<summary><h2>2026-08-14</h2></summary>

2026-07-14 에 최초 github repository 생성을 시작으로 오늘로서 1개월이 되었다.  
느낀 점은 `이런 식으로 하다간 영영 완성하지 못할 것 같다` 이다.  
개발 속도가 느린 이유를 정리해봤다.  
- 추상적인 요구사항  
명확하지 않아서 마음대로 해석해서 개발하다 보니 실제 `Amazon.com` 과 차이가 생겼다.  
예를 들어 회원가입할 때 이메일과 비밀번호만 받도록 하였다.  
`Amazon.com` 회원가입을 확인해 보니 주소와 전화번호를 요구했다.  
그래서 일단 회원가입은 이메일, 비밀번호만 받고 회원정보 화면에서 수정하도록 구현하려고 했다.  
이런 식으로 내 마음대로 구현하다 보니 `clone coding` 이라는 말이 무색해졌다.  
- 경계가 불분명한 도메인 설계 및 이해 부족  
최초 최상위 도메인 6개를 정의했다, 유저-인증, 상품, 장바구니, 쿠폰, 결제, 인프라.  
도메인 이해가 부족했던 것이 상품 등록할 때 `catalog` 라는 것이 미리 존재해야 한다.  
`catalog` 는 상품에 대한 meta data 이다.  
예를 들어 `Nvidia 5080` 에 대한 catalog 가 미리 등록되어 있고 판매자는 `Gigabyte 5080 white` 이렇게 상품 등록을 한다.  
또한 유저와 인증을 한 문서에 작성했는데 사실 분리했어야 했다.  
- agent 의 도움을 받으면서 직접 코드 작성  
직접 코드를 작성하다가 막히는 부분을 agent 에게 물어본다.  
이 과정이 생각보다 오래 걸린다.  
예를 들어 `Oauth2Provider`를 구현할 때 여러 provider 를 고려한다.  
provider 마다 반환되는 oauth 응답이 다르지만 생성해야 하는 `attributes` 는 동일해야 한다.  
이 과정에서 비슷한 혹은 구조를 가지게 되어 어떤 디자인 패턴을 사용하면 좋을지 고민을 했다.  
스스로 고민하면서 시간을 많이 보냈다(고민하는 과정이 재밌어서..).  
실제로 원하는 구조는 구현하지 못해 이걸 agent 에게 맡겼다.  
고민하는 시간이 많은 건 괜찮다고 생각하지만 생산성이 너무 떨어진다.  

느낀 점은 꼼꼼하고 구체적인 설계는 적어도 개발 속도가 음수는 아니라는 것이다.  
계획이 바뀌어 적어도 퇴보하지 않는다는 것이다.  
앞으로 계획은 `docs/requirement` 내 도메인들을 좀 더 작게 나누고 `Amazon.com` 과 비교하여 구현 계획을 세울 것이다.  
까다로운 구현은 심화과정으로 분리하여 요구사항을 구현할 때 확장을 고려할 것이다.  
생산성을 높이기 위해 실질 구현 대부분을 agent 에게 위임하고 나는 그 결과를 감독할 것이다.  
요구사항을 github issue 에 등록하여 issue 단위로 구현할 것이다.  

</details>

<details>

<summary><h2>2026-08-15</h2></summary>

동시 접속은 허용하되 하나의 주문은 한 기기에서만 주문 조회·갱신·결제를 허용한다.  
주문 세션은 영속 데이터가 아닌 Caffeine 기반의 임시 점유 정보로 관리하며 서버 재시작 시 초기화한다.  
`OrderSession`은 `orderId`, `userId`, `tokenHash`, `expiresAt`, `heartbeatExpiresAt`만 가진다.  
Checkout Token은 서버가 생성한 32바이트 난수이며 원문은 HttpOnly Cookie에만 저장하고 서버에는 SHA-256 해시만 저장한다.  
`tokenHash`는 서버가 현재 유효한 주문 세션을 직접 통제하기 위한 값이며 `deviceId`는 보안 검증에 사용하지 않는다.  
Refresh Token은 서명과 사용자 정보를 검증하기 위해 JWT를 사용하지만 주문 세션은 즉시 해제와 단일 기기 점유가 필요하므로 랜덤 토큰과 `tokenHash`를 사용한다.  
주문 관련 API 활동이 있으면 `expiresAt`을 30분 연장하고 하트비트는 브라우저 생존 확인을 위해 `heartbeatExpiresAt`만 3분 연장한다.  
주문 화면에서 사용하는 API allowlist는 세션을 유지하고 만료 시간을 연장하며 allowlist 외 API 응답은 해당 토큰의 세션과 Cookie를 정리한다.  
SPA·CSR에서는 별도 진입 endpoint를 만들지 않고 기존 화면 API 응답의 `Set-Cookie`로 현재 브라우저의 Checkout Cookie를 삭제한다.  
세션 정리는 `userId`가 아닌 `tokenHash` 기준으로 수행하므로 다른 기기의 주문 세션에는 영향을 주지 않는다.

</details>

<details>

<summary><h2>2026-08-19</h2></summary>

```java
// AddressService.java
  @Transactional
  public void delete(UUID userId, UUID addressId) {
    validateEnabledUser(userId); // ...(1)
    Address address = findOwnedAddress(userId, addressId); // ...(2)
    boolean wasPrimary = address.isPrimary();
    repository.delete(address); // ...(3)

    if (wasPrimary) {
      repository.findFirstByUserIdOrderByLastUsedAtDescCreatedAtDescIdDesc(userId) // ...(4)
          .ifPresent(nextAddress -> repository.makePrimaryByIdAndUserId(nextAddress.getId(), userId)); // ...(5)
    }
  }
```

위 코드는 `AddressService`의 주소 삭제를 수행한다.  
삭제 대상이 기본 배송지이고 승격할 주소가 존재하면 최대 5개의 SQL이 실행된다.  
사용자 활성 여부 확인 SELECT, 삭제 대상 Address SELECT, DELETE, 다음 Address SELECT, 기본 배송지 UPDATE 순서다.  
`repository.delete(address)`는 즉시 DELETE SQL을 실행하지 않고 삭제를 예약한다.  
이후 (4)의 Address 조회 전에 Hibernate의 AUTO flush가 발생하면서 DELETE SQL이 실행된다.  
(5)는 `@Modifying(flushAutomatically = true)` bulk update이므로 실행 전에 flush를 호출하고, 이후 UPDATE SQL을 즉시 실행한다.  
query 5번과 flush 2번이 발생하고 있어 이게 과연 최선인지 혹은 개선할 수 있는지 고민하고 있다.  

</details>

<details>

<summary><h2>2026-08-20</h2></summary>

`2026-08-19` 를 개선했다.  
query dsl 로 이 문제를 해결했다.  
`AddressQuerydslRepository` 를 정의했다.  
삭제한 `Address` 와 primary 가 될 `Address` 2개의 데이터만 가져왔다.  
`EntityManager` 로 삭제 query 를 생성하고 나머지 하나는 기본 배송지로 수정했다.  
현 프로젝트의 정책상 다중 접속을 허용하고 있기 때문에 동시성 문제를 고려하여 `userId` 에 해당하는 모든 `Address` 들을 비관적 락을 걸었다.  
`AddressService.makePrimary(...)` 도 이와 비슷하게 문제를 해결했다.  
`userId` 에 해당하는 모든 `Address` 에 비관적 락을 걸고 기본 배송지로 설정할 `Address` 와 기존 기본 배송지를 가져왔다.  
락은 transaction 이 끝나면 풀린다.  

</details>
