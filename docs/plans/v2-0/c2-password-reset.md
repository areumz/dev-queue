# v2-0c-2 비밀번호 찾기·이메일 인증 구현 계획

> 설계 문서: [docs/brainstorming/v2-0/c2-password-reset-brainstorming.md](../../brainstorming/v2-0/c2-password-reset-brainstorming.md)

**목표:** 이메일 소유를 인증 메일로 확인하고, 인증된 이메일로 비밀번호를 잊은 사용자가 스스로 재설정할 수 있다.

**방식:** 메일 발송은 `MailSender` 인터페이스 뒤에 숨기고(로컬·테스트는 로그, 운영은 Gmail SMTP), 인증과 재설정이 같은 "1회용 토큰" 구조(`email_token` 테이블, 해시만 저장, 만료, 횟수 제한)를 쓴다. 이메일 중복은 저장 때가 아니라 인증 때 검사하고, 인증된 이메일만 유일하다. 비밀번호 찾기의 계정 조회·토큰 발급·발송은 통째로 `@Async`로 넘겨서 응답 시간으로 가입 여부가 드러나지 않게 한다. PR은 1개, Task 4개다.

**기술:** Java 25, Spring Boot 4.1.1, Spring Security 7, `spring-boot-starter-mail`(Gmail SMTP), `@Async`, Spring Data JPA, Testcontainers MySQL

## 공통 제약

- 브랜치: `feature/v2-0c2-password-reset`(main에서 분기, 설계 문서가 이미 커밋돼 있다).
- 커밋은 검토 후 Task별로 한다. **실패하는 테스트 상태로는 커밋하지 않는다.**
- 테이블 변경: `user.email_verified` 칸 추가, 새 테이블 `email_token`. `ddl-auto`, `show-sql`, Flyway는 건드리지 않는다(d에서).
- `app.mail.enabled`와 `app.base-url`은 **기본값이 없다.** `application.properties`에 넣지 않는다. 없으면 앱이 시작되지 않는다. `LogMailSender`는 `false`를 명시했을 때만 쓴다. `true`·`false`가 아닌 값(`yes` 등)도 시작 실패다.
- `app.mail.enabled=true`이면 `app.base-url`은 `https://`로 시작해야 하고 SMTP 설정(`spring.mail.host`)이 있어야 한다. 아니면 시작되지 않는다.
- 토큰: 안전한 난수 32바이트, DB에는 SHA-256 해시(hex)만 저장. 만료는 인증 24시간, 비밀번호 재설정 30분. 1회용. 새로 발급하면 같은 사용자·같은 종류의 이전 토큰은 무효. 횟수 제한은 같은 사용자·같은 종류 **1시간에 3회**.
- 토큰 실패(없음, 만료, 이미 사용)는 이유를 구분하지 않고 같은 안내를 보여준다. 토큰과 이메일 원문은 로그·화면 응답에 남기지 않는다.
- 이메일 중복 규칙: 인증된 이메일만 유일. 미인증 이메일은 여러 계정이 가질 수 있다. 중복 검사는 **인증하는 순간**에만 한다. 저장 때 중복 오류는 없다.
- 비밀번호 찾기(`POST /forgot-password`)는 항상 같은 응답이다. 가입 여부, 미인증 여부, 횟수 제한 여부를 구분하지 않는다. 메일 처리는 `@Async("mailExecutor")`로 응답과 분리한다.
- 마이페이지의 인증 메일(가입·이메일 저장·재발송)은 동기다. 실패해도 가입·저장은 성공하고 안내만 한다.
- 로그인 없이 접근을 허용하는 경로는 `/verify-email`, `/forgot-password`, `/reset-password` 세 개뿐이다. 나머지는 로그인이 필요하고, 모든 POST는 CSRF가 필요하다.
- 재설정 성공 후 자동 로그인하지 않고 `/login?reset`으로 보낸다. 다른 기기의 로그인 세션은 유지된다(알려진 한계, c-1과 같음).
- 비밀번호 규칙은 c-1의 `@ValidPassword`를 그대로 쓴다.
- 비즈니스 로직은 Service에 둔다. Repository에 메서드·인터페이스를 추가하면 `jpa/` 어댑터, Spring Data 인터페이스, `memory/` 구현까지 4곳을 맞춘다. 외부 서비스(메일)는 인터페이스 뒤에 둔다.
- git worktree를 쓰지 않는다. 코드판 계획과 `application-local.properties`가 gitignore라서 worktree에 없다.
- CI job 이름 `code-quality`를 바꾸지 않는다. 마지막에 `./gradlew checkstyleMain spotbugsMain test`가 통과해야 한다(Docker 필요).

## 주의해서 볼 곳

1. **`@Value`로 boolean을 받으면 `yes`가 `true`가 된다.** Spring의 변환기는 `yes`, `on`, `1`도 참으로 읽는다. 설계는 오타도 시작 실패여야 하므로 문자열로 받아 `"true"`·`"false"`만 인정한다. Task 1.
2. **`@Async`를 테스트에서 동기로 돌리는 방법.** `mailExecutor` 빈을 프로필로 나눈다(`test`면 동기 실행기, 아니면 진짜 실행기). 비동기 계약 테스트용으로 `async-real` 프로필을 하나 더 둔다. Spring Boot는 같은 이름 빈 덮어쓰기가 기본 꺼져 있어서 "덮어쓰기"가 아니라 "프로필로 나누기"를 쓴다. Task 1.
3. **설계와 달라지는 한 곳: 비동기 계약 테스트는 DB를 쓰지 않는다.** 설계는 "`@Transactional`을 쓰지 않고 데이터를 직접 지운다"였다. 계획에서는 `ApplicationContextRunner` + 메모리 Repository로 같은 증명을 하므로 지울 데이터도, 별도 MySQL 컨테이너도 필요 없다. 증명하는 내용(발송이 끝나기 전에 응답, 풀면 도착)은 같다. Task 4.
4. **이메일을 여러 계정이 가질 수 있게 되면 `findByEmail`(한 명만 반환)이 예외를 던진다.** 같은 값의 행이 둘이면 `Optional` 조회가 실패한다. `findByEmail`을 없애고 "인증된 것만, 목록으로" 찾는 메서드로 바꾼다. 동시에 두 계정이 같은 이메일을 인증하는 경합이 생겨도 비밀번호 찾기는 **정확히 한 계정일 때만** 메일을 보낸다. Task 3, 4.
5. **이메일을 바꾸면 옛 인증 링크는 횟수 제한에 걸려도 무효여야 한다.** 설계는 "새 토큰 발급이 이전 토큰을 무효화한다"에 기댔지만, 횟수 제한에 걸린 발급은 이전 토큰을 안 건드린다. 그러면 옛 이메일용 링크로 **새 이메일을 인증**할 수 있다. 이메일이 바뀌면 먼저 무효화하고 그다음 발송을 시도한다. Task 3에 이 경우의 테스트를 둔다.
6. **횟수 제한 확인은 무효화보다 먼저다.** 제한에 걸린 요청이 사용자의 유효한 링크를 죽이면 안 된다. Task 2.
7. **토큰이 화면·로그·주소 이력으로 새면 안 된다.** 토큰이 들어가는 페이지(`tokenResult`, `resetPassword`)에 `<meta name="referrer" content="no-referrer">`를 둔다. 실패 페이지는 토큰을 되돌려 보여주지 않는다. 메일 실패 로그는 예외 클래스 이름만 남긴다. Task 3, 4.
8. **응답이 같아야 하므로 이메일을 화면에 되돌려 보여주지 않는다.** "입력하신 주소로"라고만 쓴다. Task 4.
9. **c-1 테스트 2개가 새 규칙과 부딪힌다.** `SignupEmailTest`의 "중복 이메일 거절", `MyPageTest`의 "남의 이메일 거절"은 이제 "허용"이 맞다. 의도된 변경으로 고친다. Task 3.
10. **`emailVerified` NOT NULL 칸 추가.** `ddl-auto=update`가 기존 행을 `false`(0)로 채우는지 배포 전에 로컬 MySQL로 확인한다.
11. **`UserService.updateProfile`의 반환값이 바뀐다.** 이메일이 바뀌었는지(`boolean`)를 돌려준다. 호출은 `MyPageController` 한 곳이다.
12. **설정이 새로 필수가 되어 로컬 `application-local.properties`도 갱신해야 한다.** 안 하면 로컬 앱이 시작되지 않는다.
13. **Memory 구현도 컴파일돼야 한다.** `memory` 프로필은 쓰지 않지만 인터페이스를 구현하고, Task 4의 비동기 계약 테스트가 이 구현을 쓴다.

## 바뀌는 파일

| 파일                                                                                           | 작업      | 역할                                                     | Task |
| ---------------------------------------------------------------------------------------------- | --------- | -------------------------------------------------------- | ---- |
| `build.gradle`                                                                                 | 수정      | `spring-boot-starter-mail`                               | 1    |
| `mail/MailSender.java`, `LogMailSender.java`, `SmtpMailSender.java`, `MailLinks.java`          | 생성      | 메일 발송 인터페이스와 구현, 링크 만들기                 | 1    |
| `config/MailConfig.java`, `AsyncConfig.java`                                                   | 생성      | 설정 검사와 발송기 선택, `mailExecutor`                  | 1    |
| `exception/MailDeliveryException.java`                                                         | 생성      | 발송 실패                                                | 1    |
| `application-test.properties`, `application-local.properties.example`, `CLAUDE.md`             | 수정      | 새 필수 설정                                             | 1    |
| `domain/EmailTokenType.java`, `EmailToken.java`                                                | 생성      | 토큰 종류·만료 시간, 토큰 행                             | 2    |
| `repository/EmailTokenRepository.java` + `jpa/` + `memory/`                                    | 생성      | 토큰 저장·조회                                           | 2    |
| `service/TokenService.java`                                                                    | 생성      | 발급, 해시, 무효화, 검증, 횟수 제한                      | 2    |
| `domain/User.java`                                                                             | 수정      | `emailVerified`, `verifyEmail`, 이메일이 바뀌면 미인증   | 3    |
| `repository/UserRepository.java` + `jpa/` + `memory/`                                          | 수정      | `findByEmail` → `findVerifiedByEmail`                    | 3    |
| `service/UserService.java`                                                                     | 수정      | 저장 때 중복 검사 제거, `updateProfile`이 `boolean` 반환 | 3    |
| `service/EmailVerificationService.java`                                                        | 생성      | 인증 메일 발송, 링크 확인, 인증 때 중복 검사             | 3    |
| `exception/InvalidTokenException.java`, `DuplicateEmailException.java`                         | 생성·수정 | 토큰 실패, 문구 변경                                     | 3    |
| `controller/EmailVerificationController.java`, `VerificationMessages.java`                     | 생성      | `GET /verify-email`, 재발송, 안내 문구                   | 3    |
| `controller/UserController.java`, `MyPageController.java`                                      | 수정      | 가입·저장 때 발송, 상태 표시                             | 3    |
| `templates/tokenResult.html`, `mypage.html`                                                    | 생성·수정 | 결과 페이지, 인증 상태와 재발송 버튼                     | 3    |
| `config/SecurityConfig.java`                                                                   | 수정      | 허용 경로 추가                                           | 3, 4 |
| `service/PasswordResetService.java`                                                            | 생성      | `@Async` 요청 처리, 재설정                               | 4    |
| `controller/PasswordResetController.java`, `ForgotPasswordForm.java`, `ResetPasswordForm.java` | 생성      | `/forgot-password`, `/reset-password`                    | 4    |
| `templates/forgotPassword.html`, `forgotPasswordSent.html`, `resetPassword.html`, `login.html` | 생성·수정 | 화면, 로그인 링크·안내                                   | 4    |
| `docs/brainstorming/v2-0/README.md`                                                            | 수정      | c-2 상태 갱신                                            | 4    |
| 테스트                                                                                         | 생성·수정 | Task별 아래 참고                                         | 1~4  |

---

## Task 1: 메일 발송 틀과 설정 검사

**해결하는 문제:** 설계 4-2 `MailSender`, 이슈 7, 이슈 6(실행기)

- [ ] 실패하는 테스트를 먼저 쓴다.
  - 설정 검사(`ApplicationContextRunner`): `app.mail.enabled`가 없으면 시작 실패, `true`·`false`가 아닌 값(`yes`)도 실패, `false`면 `LogMailSender`, `true`+`https://`+SMTP 빈이면 `SmtpMailSender`, `true`인데 `http://`면 실패, `true`인데 SMTP 설정이 없으면 실패.
  - `application.properties`에 `app.mail.enabled`·`app.base-url`이 들어 있지 않음.
  - 실행기: 대기 큐가 가득 차면 요청을 버리고 예외 없이 계속된다(작은 실행기, latch로 순서 고정).
- [ ] 실패 확인(컴파일 실패).
- [ ] `spring-boot-starter-mail`을 추가한다. `MailSender`, `LogMailSender`, `SmtpMailSender`, `MailDeliveryException`, `MailLinks`를 만든다.
- [ ] `MailConfig`(설정이 없거나 잘못되면 시작 실패), `AsyncConfig`(`mailExecutor`: 스레드 2, 큐 100, 가득 차면 버리고 경고 로그. `test` 프로필은 동기 실행기, `async-real` 프로필이면 진짜 실행기).
- [ ] 테스트 설정: `application-test.properties`에 `app.mail.enabled=false`, `app.base-url`. 메일을 모아두는 가짜 `MailSender`(`RecordingMailSender`, 실패 흉내 포함)를 만들어 `@IntegrationTest`가 쓰게 한다.
- [ ] `application-local.properties.example`에 `app.mail.enabled=false`, `app.base-url`, Gmail 설정 예시(주석, 타임아웃 포함). `CLAUDE.md` 실행 안내에 새 필수 설정을 적는다.
- [ ] 전체 테스트가 여전히 뜨는지 확인한다(기존 통합 테스트가 새 필수 설정으로 시작 실패하지 않는지). 통과 확인, 커밋(사용자 승인 후)

## Task 2: 토큰 발급·검증

**해결하는 문제:** 설계 이슈 3·4, 4-1

- [ ] 실패하는 테스트: DB에는 해시만 저장됨(원본과 다르고 64자), 발급할 때마다 다른 토큰, 유효한 토큰은 찾아짐, 없는 값·빈 값·`null`은 못 찾음, 다른 종류로는 못 찾음, 만료된 토큰은 못 찾음, 쓴 토큰은 못 찾음, 새로 발급하면 같은 사용자·같은 종류의 이전 토큰은 무효(다른 종류·다른 사용자는 그대로), 1시간 3회까지 허용·4번째는 거절되고 마지막 유효 토큰은 그대로 살아 있음, 제한은 사용자별·종류별, 1시간이 지난 발급은 세지 않음, 모든 열린 토큰 무효화, 만료 시간(인증 24시간, 재설정 30분). 메모리 Repository의 저장·조회·개수.
- [ ] 실패 확인(컴파일 실패).
- [ ] `EmailTokenType`(종류와 만료 시간), `EmailToken` 엔티티, `EmailTokenRepository`(저장, 해시+종류로 조회, 사용자의 열린 토큰 조회, 기간 내 발급 개수) 4곳.
- [ ] `TokenService`: `issue`(횟수 제한 확인 → 이전 토큰 무효화 → 난수 발급·해시 저장, 제한에 걸리면 빈 값), `findValid`, `consume`, `invalidateOpen`(한 종류), `invalidateAllOpen`(모든 종류), 정적 `hash`.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 3: 이메일 인증

**해결하는 문제:** 설계 이슈 1·2, 4-3 인증 규칙

- [ ] 실패하는 테스트:
  - 가입: 이메일이 있으면 인증 메일이 가고(링크가 `app.base-url`로 시작), 없으면 안 감, 메일 발송이 실패해도 가입은 성공하고 안내 문구가 뜬다.
  - 링크: 로그인 없이 눌러도 인증됨, 한 번만 쓸 수 있음, 없는·만료·쓴 링크는 같은 안내 페이지, 토큰 없이 열어도 같은 안내.
  - 이메일 변경: 바꾸면 미인증이 되고 새 주소로 메일이 감, 같은 이메일이나 닉네임만 바꾸면 인증 상태 유지·메일 없음, 바꾸면 옛 링크는 무효(**횟수 제한에 걸린 변경에서도**).
  - 중복: 미인증 이메일은 여러 계정이 가질 수 있고, 먼저 인증한 계정이 가져가면 다른 계정의 인증은 "이미 다른 계정에서 인증된 이메일입니다."로 실패한다.
  - 마이페이지: 인증 상태 표시, 미인증일 때만 재발송 버튼, 재발송은 메일을 보내고 1시간 3회를 넘으면 안내, 이미 인증됐거나 이메일이 없으면 보내지 않음, 비로그인은 `/login`으로, CSRF 없으면 403, 폼 화면에 `_csrf` 필드.
  - c-1 테스트 2개 수정(중복 이메일 거절 → 허용).
- [ ] 실패 확인.
- [ ] `User.emailVerified`와 `verifyEmail`, 이메일이 바뀌면 미인증으로 되돌림. `findByEmail`을 `findVerifiedByEmail`(인증된 것만, 목록)로 바꾼다(4곳). `UserService`에서 저장 때 중복 검사를 지우고 `updateProfile`이 이메일이 바뀌었는지(`boolean`)를 돌려준다.
- [ ] `InvalidTokenException`, `DuplicateEmailException` 문구 변경. `EmailVerificationService`(`sendVerification`, `sendAfterEmailChange`(먼저 옛 토큰 무효화), `verify`(인증 때 중복 검사)).
- [ ] `EmailVerificationController`(`GET /verify-email`, `POST /mypage/verify-email`), `VerificationMessages`, `tokenResult.html`. `UserController`는 가입 후 발송, `MyPageController`는 저장 후 발송과 상태 표시. `mypage.html`에 상태와 재발송 버튼. `SecurityConfig`에 `/verify-email` 허용.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 4: 비밀번호 찾기

**해결하는 문제:** 설계 이슈 6, 4-3 비밀번호 찾기·재설정 규칙

- [ ] 실패하는 테스트:
  - 요청: 인증된 이메일이면 메일이 가고, 없는·미인증 이메일은 안 가는데 **응답은 완전히 같다**, 빈 값·형식 오류는 폼 오류, 1시간 4번째 요청은 메일이 안 가지만 응답은 같다.
  - 링크: 유효하면 폼(토큰 숨은 필드), 아니면 안내 페이지. 없는·만료·쓴 링크의 안내는 같다.
  - 재설정: 새 비밀번호로 바뀌고(옛 비밀번호는 실패) `/login?reset`으로 이동, 같은 링크는 두 번 못 씀, 규칙 위반(7자)은 거부되고 링크는 그대로 쓸 수 있음, 성공하면 다른 열린 토큰(인증 링크 포함)이 무효, 인증용 토큰으로는 재설정 불가, 토큰 발급 뒤 계정이 지워졌으면 안내 페이지.
  - 보안: 두 POST는 CSRF 없으면 403, 허용된 세 경로는 비로그인으로 열리고 `/mypage`는 여전히 `/login`으로, 로그인 화면에 링크와 `?reset` 안내, 메일 발송이 실패해도 응답은 같고 로그에 토큰·이메일이 없다, 폼 화면에 `_csrf` 필드.
  - 비동기 계약(`ApplicationContextRunner` + 메모리 Repository + latch로 멈추는 가짜 `MailSender`): 요청이 발송이 끝나기 전에 돌아오고, 발송은 `mail-` 스레드에서 일어나며, 멈춤을 풀면 메일이 도착한다.
- [ ] 실패 확인.
- [ ] `PasswordResetService`: `requestReset`(`@Async("mailExecutor")`: 이메일 정리 → **인증된 계정이 정확히 하나일 때만** 토큰 발급·발송, 예외는 클래스 이름만 로그), `isTokenValid`, `resetPassword`(토큰 확인 → 비밀번호 변경 → 열린 토큰 모두 무효화).
- [ ] `ForgotPasswordForm`, `ResetPasswordForm`(`@ValidPassword`), `PasswordResetController`(`GET/POST /forgot-password`, `GET/POST /reset-password`), 화면 3개(`forgotPassword`, `forgotPasswordSent`, `resetPassword`), `login.html`에 링크와 `?reset` 안내, `SecurityConfig`에 두 경로 허용.
- [ ] README의 c-2 상태를 "구현 완료, PR 리뷰 대기"로 고친다.
- [ ] 전체 검증: `./gradlew checkstyleMain spotbugsMain test`. 통과 확인, 커밋(사용자 승인 후)

---

## 끝난 뒤

- 검토가 끝나면 푸시하고 PR을 만든다(본문은 PR #16~#18 형식). CI 확인 후 병합한다.
- **배포 전 해야할 일:** 로컬 `application-local.properties`에 `app.mail.enabled=false`, `app.base-url=http://localhost:8080` 추가. 로컬 MySQL로 `emailVerified` 칸이 기존 행에서 `false`로 채워지는지 확인. Gmail 앱 비밀번호 발급(서버 환경 변수에만). **HTTPS 적용·확인 후** 환경 변수(`APP_MAIL_ENABLED=true`, `APP_BASE_URL=https://...`, `SPRING_MAIL_*`)를 넣고 c-2를 배포한다(HTTPS → c-2 순서).
- PR 본문에 적을 것: 개선 전후, Task별 변경, 주요 결정(Gmail SMTP, 인증된 이메일만 유일, 비동기 분리, 설정 누락 시 시작 실패), 겪은 이슈, 배포 시 주의(HTTPS 먼저, 환경 변수, `emailVerified` 칸 추가, 기존 사용자는 미인증으로 시작), 알려진 한계(다른 기기 세션 유지, 인증 불가 계정은 복구 방법 없음, GET 인증 링크, IP 제한 없음, 큐 가득 차면 요청 버림).
- c-3·d로 넘기는 것: 모든 세션 끊기(`SessionRegistry`), IP 단위 횟수 제한, 이메일·아이디 DB 유니크 제약, 영속 메일 큐·재시도.
