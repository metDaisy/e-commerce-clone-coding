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
