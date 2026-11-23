# v2-0c-1 마이페이지 구현 계획

> 설계 문서: [docs/brainstorming/v2-0/c1-mypage-brainstorming.md](../../brainstorming/v2-0/c1-mypage-brainstorming.md)

**목표:** 로그인한 사용자가 자기 정보(닉네임, 직군, 이메일, 비밀번호)를 직접 고치고, 원하면 탈퇴할 수 있다. 닉네임을 바꾸면 화면에도 바로 반영된다.

**방식:** `User`에 이메일 칸(선택)을 추가하고, 마이페이지는 주소에 id 없이 로그인한 본인만 다룬다. 규칙 판단(이메일 정리·중복, 현재 비밀번호 확인, 탈퇴 삭제 순서)은 `UserService`가 맡는다. 닉네임을 바꾸면 컨트롤러 쪽 작은 도우미가 세션의 `LoginUser`를 새 값으로 교체한다. 탈퇴는 한 트랜잭션에서 단어 → 글 → 계정 순으로 지운다. PR은 1개, Task 5개다.

**기술:** Java 25, Spring Boot 4.1.1, Spring Security 7(폼 로그인), Bean Validation(`@Email`, 직접 만든 비밀번호 규칙), Spring Data JPA(JPQL 삭제), Testcontainers MySQL

## 공통 제약

- 브랜치: `feature/v2-0c1-mypage`(main에서 분기, 설계 문서가 이미 커밋돼 있다).
- 커밋은 사용자 승인 후 Task별로 한다. **실패하는 테스트 상태로는 커밋하지 않는다.**
- 테이블 변경은 `user.email` 칸 하나뿐이다(비워도 되는 칸). `ddl-auto`, `show-sql`, Flyway는 건드리지 않는다(d에서). 새 테이블은 없다.
- 이메일 소유 확인(인증 메일)은 하지 않는다 → c-2. 이메일은 저장만 하고 어떤 기능에도 연결하지 않는다.
- `email`·`username` DB 유니크 제약은 걸지 않는다 → d. 중복 검사는 앱 코드에서만 한다.
- 모든 로그인 사용자 전용 화면은 id를 주소로 받지 않고 `@AuthenticationPrincipal LoginUser`의 id만 쓴다. 남의 것에 접근할 방법이 없다.
- 지워진 계정의 남은 세션으로 마이페이지를 열면 404다(403을 쓰지 않는다).
- 비즈니스 로직은 Service에 둔다. 컨트롤러는 화면·폼 검증 결과·리다이렉트만 맡는다.
- Repository 인터페이스에 메서드를 추가하면 `jpa/` 어댑터, Spring Data 인터페이스, `memory/` 구현까지 4곳을 맞춘다.
- 모든 폼은 `th:action`을 쓴다(CSRF 토큰 자동 삽입).
- 이메일은 서비스의 한 메서드에서만 정리한다: 앞뒤 공백 제거 → 소문자 → 빈 값은 `null`.
- 비밀번호 규칙: 8자 이상, 72바이트 이하(UTF-8). 가입과 비밀번호 변경에 같은 규칙을 쓰고, 이미 저장된 비밀번호의 로그인은 막지 않는다.
- 화면 문구: "이미 존재하는 아이디입니다.", "이미 사용 중인 이메일입니다.", "이메일 형식이 올바르지 않습니다.", "비밀번호는 8자 이상, 72바이트 이하로 입력해 주세요(한글은 약 24자까지).", "비밀번호가 올바르지 않습니다.", 가입 폼 안내 "등록하지 않으면 비밀번호를 잊었을 때 되찾기 어려워요."
- 다른 기기의 로그인 세션은 유지된다(알려진 한계, 설계 3장). `SessionRegistry`는 만들지 않는다.
- git worktree를 쓰지 않는다. 코드판 계획과 `application-local.properties`가 gitignore라서 worktree에 없다.
- CI job 이름 `code-quality`를 바꾸지 않는다. 마지막에 `./gradlew checkstyleMain spotbugsMain test`가 통과해야 한다(Docker 필요).

## 주의해서 볼 곳

1. **`@Email`은 앞뒤 공백이 있으면 거절한다.** 입력값이 서비스에 닿기 전에 폼 단계에서 정리해야 `" A@X.com "`이 통과한다. Task 1.
2. **`null` 관련 설정. 이메일끼리는 중복이 아니다.** 빈 값을 `null`로 바꾸지 않으면 이메일 없이 두번째 가입하려는 사람부터 가입이 막히게 된다. Task 1에 테스트.
3. **비밀번호 규칙 때문에 기존 테스트가 깨질 수 있다.** 가입 요청에 8자 미만 비밀번호(예: `pw1234`)를 쓰는 테스트를 찾아 고친다. Task 1.
4. **세션 갱신은 Security 7에서 저장을 직접 해야 한다.** `SecurityContextHolder`에 넣는 것만으로는 세션에 남지 않는다. 같은 세션으로 다음 요청을 보내 새 닉네임이 보이는지로 증명한다. Task 3.
5. **DB에서 지워도 JPA 메모리 캐시에는 남을 수 있다.** JPA는 읽어 온 행을 한 요청 동안 메모리(영속성 컨텍스트)에 들고 있는데, 한 번에 지우는 JPQL 삭제는 이 캐시를 모른다. 지운 뒤에도 조회하면 캐시의 옛 값이 나올 수 있어서, 삭제 쿼리에 `flushAutomatically`(삭제 전 저장 대기분 반영)와 `clearAutomatically`(삭제 후 캐시 비우기)를 켠다. Task 5.
6. **탈퇴하면 세션이 끊겨서 플래시 메시지(리다이렉트 한 번만 보이는 안내 문구)를 못 쓴다.** 플래시 메시지는 세션에 잠깐 저장되기 때문이다. 대신 `/login?withdrawn` 주소로 이동해 로그인 화면이 안내 문구를 띄운다. Task 5.
7. **지워진 사용자의 남은 세션**으로 `/mypage`를 열면 404여야 한다. Task 2.
8. **새 폼 3개 모두 `_csrf` 숨은 필드가 있어야 한다.** `FormCsrfFieldTest`에 추가한다. Task 2, 4, 5.
9. **Memory 구현도 컴파일돼야 한다.** `memory` 프로필은 쓰지 않지만 인터페이스를 구현하므로 빠뜨리면 빌드가 깨진다.

## 바뀌는 파일

| 파일                                                                                  | 작업      | 역할                                                                       | Task       |
| ------------------------------------------------------------------------------------- | --------- | -------------------------------------------------------------------------- | ---------- |
| `domain/User.java`                                                                    | 수정      | `email` 칸, `getRole`, `updateProfile`, `changePassword`                   | 1, 2, 4    |
| `exception/DuplicateUsernameException.java`, `DuplicateEmailException.java`           | 생성      | 가입 오류를 칸별로 구분                                                    | 1          |
| `exception/WrongPasswordException.java`                                               | 생성      | 현재 비밀번호 불일치                                                       | 4          |
| `validation/ValidPassword.java`, `PasswordRuleValidator.java`                         | 생성      | 비밀번호 길이 규칙 한 곳                                                   | 1          |
| `service/UserService.java`                                                            | 수정      | 이메일 정리·중복, `getUser`, `updateProfile`, `changePassword`, `withdraw` | 1, 2, 4, 5 |
| `controller/SignupForm.java`, `UserController.java`                                   | 수정      | 이메일 입력, 새 예외 처리                                                  | 1          |
| `templates/signup.html`                                                               | 수정      | 이메일 입력과 안내문                                                       | 1          |
| `repository/UserRepository.java` + `jpa/` + `memory/`                                 | 수정      | `findByEmail`, `deleteById`                                                | 1, 5       |
| `repository/ArticleRepository.java`, `VocabularyRepository.java` + `jpa/` + `memory/` | 수정      | `deleteByUserId`                                                           | 5          |
| `controller/MyPageForm.java`, `MyPageController.java`                                 | 생성      | 마이페이지 조회·수정, 이후 비밀번호·탈퇴                                   | 2, 4, 5    |
| `controller/PasswordChangeForm.java`                                                  | 생성      | 비밀번호 변경 폼                                                           | 4          |
| `templates/fragments/header.html`, `mypage.html`                                      | 생성      | 공통 상단, 마이페이지                                                      | 2          |
| `templates/articles.html`                                                             | 수정      | 닉네임이 마이페이지 링크                                                   | 2          |
| `auth/LoginUserRefresher.java`                                                        | 생성      | 세션의 `LoginUser` 교체                                                    | 3          |
| `templates/passwordChange.html`                                                       | 생성      | 비밀번호 변경 화면                                                         | 4          |
| `templates/withdraw.html`, `login.html`                                               | 생성·수정 | 탈퇴 확인 화면, 탈퇴 안내 문구                                             | 5          |
| `docs/brainstorming/v2-0/README.md`                                                   | 수정      | c-1 상태 갱신                                                              | 5          |
| 테스트                                                                                | 생성·수정 | Task별 아래 참고                                                           | 1~5        |

---

## Task 1: 이메일 칸, 정리, 비밀번호 길이 규칙, 가입 오류 분리

**해결하는 문제:** 설계 이슈 2·4, 4-1, 4-1-1

- [ ] 실패하는 테스트를 먼저 쓴다.
  - 이메일 정리 단위 테스트: `null`, 빈 값, 공백뿐인 값은 `null`. `" A@X.com "`은 `a@x.com`.
  - 비밀번호 규칙 단위 테스트: 7자 거절, 8자 허용, 영문 72바이트 허용·73바이트 거절, 한글 24자 허용·25자 거절, 빈 값은 통과(빈 값은 `@NotBlank`가 잡는다).
  - 가입 통합 테스트: 이메일이 정리돼 저장됨, 공백뿐인 이메일은 `null`, 대소문자·공백만 다른 이메일은 "이미 사용 중인 이메일입니다.", 형식 오류 문구, 이메일 없는 두 계정은 둘 다 가입, 7자 비밀번호 거절·8자 허용.
- [ ] 실패 확인(컴파일 실패 또는 assertion 실패).
- [ ] `User`에 `email` 칸과 6개 인자 생성자를 추가한다(기존 5개 인자 생성자는 그대로 둬서 기존 테스트를 안 건드린다). `UserRepository.findByEmail`을 4곳에 추가한다.
- [ ] 예외 두 개(`DuplicateUsernameException`, `DuplicateEmailException`)와 비밀번호 규칙(`@ValidPassword`)을 만든다.
- [ ] `UserService`: `normalizeEmail`(정적 메서드, 정리는 여기 한 곳), `signup`이 이메일을 받아 정리 후 중복 검사. 예외는 새 예외 두 개를 던진다.
- [ ] `SignupForm`에 이메일과 `@ValidPassword`, `UserController`는 새 예외를 칸별로 붙이고 기존 `IllegalStateException` 처리는 지운다. `signup.html`에 이메일 입력과 안내문.
- [ ] 기존 테스트 정리: 8자 미만 비밀번호로 가입하는 테스트(`InputValidationTest`), `signup(...)` 호출부(`LoginFlowTest` 등)를 고친다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 2: 마이페이지 조회·수정

**해결하는 문제:** 설계 1장 `GET/POST /mypage`, 이슈 3의 일부(지워진 계정 404)

- [ ] 실패하는 테스트: 비로그인 `/mypage`는 `/login`으로, 로그인하면 아이디·닉네임·이메일이 채워진 폼, 수정하면 닉네임·직군·이메일이 바뀜(이메일은 정리됨), 이메일을 비우면 `null`, 남의 이메일은 "이미 사용 중인 이메일입니다."이고 아무것도 안 바뀜, 자기 이메일 그대로 저장은 허용, 닉네임이 비면 오류, 형식 오류, CSRF 없는 POST는 403, 지워진 계정의 세션은 404, 폼 화면에 `_csrf` 필드.
- [ ] `User`에 `getRole`, `updateProfile`. `UserService`에 `getUser`(없으면 404 예외), `updateProfile`(이메일 정리·중복 검사 후 변경).
- [ ] `MyPageForm`, `MyPageController`(`GET/POST /mypage`), 공통 상단 조각(`fragments/header.html`), `mypage.html`. `articles.html`의 닉네임을 마이페이지 링크로 바꾼다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 3: 닉네임 변경 시 세션 갱신

**해결하는 문제:** 설계 이슈 1(A안)

- [ ] 실패하는 테스트: 실제 로그인한 세션으로 닉네임을 바꾼 뒤 **같은 세션**으로 `/articles`를 열면 새 닉네임이 보이고 옛 닉네임은 안 보인다. 갱신된 세션의 `LoginUser`는 비밀번호가 없고 id·아이디는 그대로다.
- [ ] 실패 확인: 지금은 옛 닉네임이 그대로 보인다.
- [ ] `LoginUserRefresher`(새 `LoginUser`를 `SecurityContext`에 넣고 세션에 저장)를 만들고 `MyPageController`가 저장 성공 직후 호출한다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 4: 비밀번호 변경

**해결하는 문제:** 설계 1장 `/mypage/password`, 4-1-1

- [ ] 실패하는 테스트: 성공하면 새 비밀번호로 로그인되고 옛 비밀번호는 `/login?error`, 현재 비밀번호가 틀리면 오류 문구가 보이고 비밀번호는 그대로, 새 비밀번호가 짧거나(7자) 길면(한글 25자) 같은 규칙으로 거절, 성공하면 현재 브라우저의 세션 ID가 바뀜, 비로그인은 `/login`으로, 폼 화면에 `_csrf` 필드.
- [ ] `User.changePassword`, `WrongPasswordException`, `UserService.changePassword`(현재 비밀번호 확인 후 새 해시 저장).
- [ ] `PasswordChangeForm`(`@ValidPassword`), 컨트롤러 `GET/POST /mypage/password`, `passwordChange.html`, 마이페이지에 링크. 성공하면 세션 ID를 재발급한다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 5: 회원 탈퇴

**해결하는 문제:** 설계 이슈 3, 4-3 탈퇴 규칙

- [ ] 실패하는 테스트: 두 사용자(각자 글·단어 보유) 중 한 명이 탈퇴하면 **그 사용자의 단어·글·계정만** 사라지고 다른 사용자의 것은 그대로, 비밀번호가 틀리면 아무것도 안 지워짐, 탈퇴 후 그 아이디로 로그인 불가, 탈퇴 후 세션이 끊김, `/login?withdrawn`으로 이동해 안내 문구, CSRF 없는 POST는 403, 비로그인은 `/login`으로, 폼 화면에 `_csrf` 필드.
- [ ] `VocabularyRepository.deleteByUserId`, `ArticleRepository.deleteByUserId`, `UserRepository.deleteById`를 4곳에 추가한다(JPQL 일괄 삭제, 영속성 컨텍스트 정리 옵션 포함).
- [ ] `UserService.withdraw`(`@Transactional`: 비밀번호 확인 → 단어 → 글 → 계정). 컨트롤러 `GET/POST /mypage/withdraw`, `withdraw.html`, 마이페이지에 링크, `login.html`에 안내 문구.
- [ ] README의 c-1 상태를 "구현 완료, PR 리뷰 대기"로 고친다.
- [ ] 전체 검증: `./gradlew checkstyleMain spotbugsMain test`. 통과 확인, 커밋(사용자 승인 후)

---

## 끝난 뒤

- 사용자가 모든 커밋을 승인하면 푸시하고 PR을 만든다(본문은 PR #16·#17 형식). 머지는 사용자가 한다.
- PR 본문에 적을 것: 개선 전후, Task별 변경, 주요 결정, 겪은 이슈, 배포 시 주의(`user.email` 칸 추가, 비밀번호 규칙은 가입·변경 때만, **탈퇴는 복구 불가**, 다른 기기 세션 유지, 탈퇴 후 남은 세션으로 주인 없는 글 생성 가능), 알려진 한계.
- c-2로 넘기는 것: 이메일 소유 확인. d로 넘기는 것: `email`·`username` 유니크 제약.
