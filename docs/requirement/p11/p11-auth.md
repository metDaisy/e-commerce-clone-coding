# P11 Auth 문서 분리 안내

P11 Auth API는 생명주기와 책임 경계를 기준으로 다음 문서로 분리했다.

| 문서 | 책임 |
|---|---|
| [Credential API](p11-credential.md) | 로컬·소셜 인증수단, 비밀번호 검증·변경, 재인증된 내 인증수단 요약 조회 |
| [Sign-up API](p11-signup.md) | 로컬·소셜 가입, 이메일 OTP, Guest Token |
| [Session API](p11-session.md) | 로그인, Access·Refresh Token, 로그아웃·세션 무효화 |
| [P11 Policy](p11-policy.md) | 세 문서에 공통으로 적용되는 업무 규칙·불변식 |

전체 문서 목록과 도메인 경계는 [P11 Index](p11-index.md)를 참고한다.
