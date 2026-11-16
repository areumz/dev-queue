# v2-0b 버그·보안 수정 + 최소 Spring Security 구현 계획

> 설계 문서: [docs/brainstorming/v2-0/b-security-fixes-brainstorming.md](../../brainstorming/v2-0/b-security-fixes-brainstorming.md)

**목표:** 로그인한 사용자가 자기 데이터만 보고 고칠 수 있고, 다른 사이트가 사용자 몰래 요청을 보낼 수 없다. 브리프 1장의 코드 버그를 함께 고친다.

**방식:** Spring Security의 폼 로그인이 로그인·CSRF·세션 ID 재발급을 맡고, 세션에는 비밀번호 없는 `LoginUser`만 남는다. 주인 확인은 Service가 로그인 사용자 id를 받아서 하고, 남의 것과 없는 것은 모두 `ResourceNotFoundException`(404)이다. PR 2개로 나눈다: PR 1 보안(Task 1~4), PR 2 로직·구조(Task 5~9).

**기술:** Java 25, Spring Boot 4.1.1, `spring-boot-starter-security`, `spring-boot-starter-security-test`(둘 다 4.1.1 존재 확인함, 버전은 Spring Boot가 관리), Testcontainers MySQL, Hibernate 통계(쿼리 수 측정)

## 공통 제약

- 브랜치: `feature/v2-0b-security-fixes`에서 PR 1. PR 1 머지 후 main에서 `feature/v2-0b-logic-fixes`를 새로 따서 PR 2.
- 커밋은 사용자 승인 후 Task별로 한다. **실패하는 테스트 상태로는 커밋하지 않는다.**
- 테이블 구조를 바꾸지 않는다. `ddl-auto`, `show-sql`, Flyway는 건드리지 않는다(d에서).
- 남의 리소스와 없는 리소스는 똑같이 404. 403을 쓰지 않는다.
- 주인 확인은 Service에서 한다. 컨트롤러에서 `userId`를 비교하지 않는다.
- 모든 폼은 `th:action`을 쓴다(CSRF 토큰 자동 삽입).
- 컨트롤러는 로그인 사용자를 `@AuthenticationPrincipal LoginUser`로 받는다. `HttpSession`을 직접 쓰지 않는다.
- Repository 인터페이스에 메서드를 추가하면 `jpa/`와 `memory/` 구현을 둘 다 맞춘다.
- 화면 문구는 설계 문서 그대로 쓴다: "아이디 또는 비밀번호가 올바르지 않습니다.", "http 또는 https 주소만 저장할 수 있어요", "이미 존재하는 아이디입니다.", "찾을 수 없는 페이지예요", "요청이 올바르지 않아요", "잠시 후 다시 시도해 주세요".
- git worktree를 쓰지 않는다. 코드판 계획과 `application-local.properties`가 gitignore라서 worktree에 없다.
- CI job 이름 `code-quality`를 바꾸지 않는다.

## 주의해서 볼 곳

1. **대문자·공백이 섞인 URL 스킴**(`JavaScript:alert(1)`, 앞에 공백 붙은 `javascript:`) → 거절돼야 한다. Task 4에 테스트 추가.
2. **호스트 없는 URL**(`https://`, `http:foo`) → 거절돼야 한다. Task 4에 테스트 추가.
3. **팝업 확인에 ids가 하나도 없음**(체크 안 하고 확인) → 아무것도 안 바뀌고 목록으로 이동. Task 3에 테스트 추가.
4. **수정·생성 폼이 URL 오류로 다시 그려질 때 다른 입력값이 사라짐** → 제목·카테고리·메모가 그대로 남아야 한다. Task 4에 테스트 추가.
5. **회원가입 검증 실패 시 직군 선택지가 사라짐**(이번에 발견, 지금도 있는 버그) → 다시 그려도 직군 목록이 보여야 한다. Task 8에 테스트 추가.
6. **빈 값·공백뿐인 URL**(운영 DB에 빈 URL 글이 4건 있다) → 저장·수정 모두 거절돼야 한다. Task 4에 테스트 추가.
7. **`th:action`이 빠진 폼** → 모든 폼 화면에 `_csrf` 숨은 필드가 있어야 한다. POST 테스트는 토큰을 직접 붙여서 이걸 못 잡는다. Task 1에 테스트 추가.
8. **세션 만료 후 폼 제출(CSRF 실패)** → 403 화면이 "세션이 만료됐어요. 다시 로그인해 주세요"를 보여 준다. Task 4에 테스트 추가.

## 바뀌는 파일

| 파일 | 작업 | 역할 | Task |
|---|---|---|---|
| `build.gradle` | 수정 | Security 의존성 추가, `spring-security-crypto` 삭제 | 1 |
| `auth/LoginUser.java` | 생성 | 세션에 담기는 로그인 사용자(id, username, nickname) | 1 |
| `auth/LoginUserDetailsService.java` | 생성 | 아이디로 사용자를 찾아 `LoginUser`로 바꿈 | 1 |
| `config/SecurityConfig.java` | 수정 | 로그인·로그아웃·접근 규칙 | 1 |
| `config/LoginCheckInterceptor.java`, `config/WebConfig.java` | 삭제 | Security가 대신함 | 1 |
| `controller/UserController.java` | 수정 | login/logout 메서드 삭제 | 1 |
| `templates/login.html` | 수정 | `th:action`, `?error` 문구 | 1 |
| `test/.../support/TestSessions.java` → `TestAuth.java` | 교체 | Security 방식 로그인 흉내 | 1 |
| `controller/ArticleController.java` | 수정 | `@AuthenticationPrincipal`, Service 새 시그니처 | 2, 3, 4 |
| `service/VocabularyService.java` | 수정 | 팝업 로직 이동, 주인 확인 | 2, 3 |
| `exception/ResourceNotFoundException.java` | 생성 | 없음/남의 것 → 404 | 3 |
| `service/ArticleService.java` | 수정 | 주인 확인, URL 검사 | 3, 4 |
| `test/.../controller/ArticleOwnershipTest.java` | 생성 | IDOR 테스트 | 3 |
| `test/.../controller/LoginFlowTest.java` | 생성 | 로그인·CSRF·세션 ID 테스트, 로그아웃 상태에서 `/error`가 열려 있는지 | 1 |
| `test/.../controller/FormCsrfFieldTest.java` | 생성 | 폼 화면마다 `_csrf` 숨은 필드가 있는지 | 1 |
| `test/.../auth/LoginUserTest.java` | 생성 | 직렬화되는지, 인증 후 비밀번호가 지워지는지 | 1 |
| `templates/error/403.html`, `test/.../controller/CsrfErrorPageTest.java` | 생성 | 세션 만료(CSRF 실패) 안내 화면 | 4 |
| `templates/createArticleForm.html`, `editArticleForm.html` | 수정 | URL 오류 문구, 입력값 유지 | 4 |
| `config/spotbugs/exclude.xml` | 수정 | v2-0b 항목 삭제 | 1 |
| `CLAUDE.md`, ADR 0003·0004 | 수정·생성 | 문서 | 4 |
| `config/ClockConfig.java` | 생성 | `Asia/Seoul` Clock | 5 |
| `repository/*`, `repository/jpa/*`, `repository/memory/*` | 수정 | DB에서 거르는 조회 | 6, 7 |
| `exception/GlobalExceptionHandler.java`, `templates/error/*.html` | 생성 | 에러 화면 | 9 |

---

# PR 1 보안

## Task 1: Spring Security로 로그인 바꾸기

**해결하는 문제:** 설계 2장 전체(CSRF, 세션 ID 재발급, 세션에 `User` 저장)

- [ ] `LoginFlowTest`에 실패하는 테스트를 먼저 쓴다.
  - 맞는 비밀번호로 로그인 → `/articles`로 이동, 세션 ID가 로그인 전과 다르다.
  - 틀린 비밀번호 → `/login?error`.
  - `/login?error` 화면에 "아이디 또는 비밀번호가 올바르지 않습니다."가 보인다.
  - 로그인 후 세션 안의 로그인 사용자는 `LoginUser`이고 비밀번호가 비어 있다. `loginUser` 세션 속성은 없다.
  - CSRF 토큰 없이 `POST /articles/new` → 403.
  - `POST /logout`(토큰 포함) → `/login`으로 이동.
  - 로그아웃 상태에서 `/error` 요청(상태 403을 흉내 냄)이 `/login`으로 이동하지 않고 403으로 응답한다.
- [ ] `FormCsrfFieldTest`: 로그인, 회원가입, 글 작성, 글 수정, 글 상세(메모·읽음·단어 추가·토글·삭제·글 삭제 폼), 팝업이 뜨는 글 목록(팝업·로그아웃 폼)을 GET으로 열어 **모든 폼**에 `name="_csrf"`가 있다.
- [ ] `LoginUserTest`: `LoginUser`를 직렬화했다가 복원해도 id·username·nickname이 같다. `eraseCredentials()` 후 비밀번호가 null이다.
- [ ] 실패를 확인한다.
- [ ] 의존성을 바꾸고 `LoginUser`, `LoginUserDetailsService`, `SecurityConfig`를 만든다. 인터셉터와 `WebConfig`를 지운다. **지우기 전에** `WebConfig`의 열린 주소(`/login`, `/signup`, `/css/**`, `/js/**`)가 `SecurityConfig`의 permitAll과 같은지 대조한다. `/error`만 새로 추가된 것이다.
- [ ] `TestSessions`를 `TestAuth.loginAs(User)`로 교체하고 `ArticleControllerTest`를 새 도우미로 고친다. 비로그인 리다이렉트는 이제 `http://localhost/login` 형태라서 기대값을 패턴(`**/login`)으로 바꾼다. 로그인 후 목록 테스트에 닉네임("앨리스님")이 화면에 보이는지 확인을 추가한다.
- [ ] `ArticleController`를 `@AuthenticationPrincipal LoginUser`로 바꾼다. 이 Task에서는 기존 Service 호출을 그대로 두고 `loginUser.getId()`만 넘긴다. 팝업은 Task 2에서 고친다(이 Task 동안 `list()`는 DB에서 사용자를 다시 읽는다). `list()`는 모델에 `loginUser`를 계속 넣는다. `articles.html`이 `loginUser.nickname`으로 닉네임을 보여 준다.
- [ ] SpotBugs 제외 목록에서 "v2-0b" 항목을 지운다(원인 코드가 사라짐).
- [ ] `./gradlew checkstyleMain spotbugsMain test` 통과 확인.
- [ ] 커밋(사용자 승인 후)

## Task 2: 팝업 로직을 Service로 옮기기

**해결하는 문제:** 설계 2-2 "팝업 날짜는 DB에서 읽는다", CLAUDE.md "Controller에 비즈니스 로직을 두지 않는다"

- [ ] 테스트를 먼저 쓴다(Task 1 상태에서 이미 통과할 수 있다. 그러면 옮기는 동안 동작이 안 바뀌는지 지켜 주는 안전망으로 둔다): 안 외운 단어가 있고 `lastPopupDate`가 비어 있는 사용자 → 목록 화면에 팝업 단어가 보이고, DB의 `lastPopupDate`가 오늘이 된다. 같은 날 두 번째 요청에는 팝업이 없다.
- [ ] `VocabularyService.takeTodayPopupWords(userId)`를 만들고 컨트롤러는 그 결과만 화면에 넘긴다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 3: 소유권 검증 (IDOR)

**해결하는 문제:** 설계 3장

- [ ] `ArticleOwnershipTest`에 설계 3-2 표의 모든 요청마다 "앨리스의 리소스에 밥이 요청 → 404, 앨리스 데이터 그대로" 테스트를 쓴다(9개 요청 + 없는 id 1개 + memorize에 남의 id가 섞인 경우).
- [ ] **전부 실패하는 것을 확인하고 결과(실패 개수와 실제 응답 코드)를 기록한다.** PR 1 본문에 쓴다. 이 상태로 커밋하지 않는다.
- [ ] `ResourceNotFoundException`(`@ResponseStatus(NOT_FOUND)`)을 만든다.
- [ ] Service 메서드들이 `userId`를 받고 주인을 확인하게 바꾼다. 단어 토글·삭제는 단어가 속한 글 id를 돌려주고, 컨트롤러는 그 id로 리다이렉트한다(폼의 `articleId` 무시).
- [ ] memorize는 ids 전체를 먼저 확인하고, 하나라도 남의 것이면 아무것도 바꾸지 않고 404.
- [ ] 정상 경로도 테스트한다: 앨리스가 자기 글을 수정·삭제·메모·읽음 처리, 단어 추가·토글·삭제 → 성공. ids가 비어 있는 memorize → 목록으로 이동.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 4: URL 검사 + 문서

**해결하는 문제:** 설계 4장 첫 줄(`javascript:` 저장), 10장 ADR, 8장 CLAUDE.md

- [ ] 실패하는 테스트: 생성·수정에서 `javascript:alert(1)`, `JavaScript:alert(1)`, ` javascript:alert(1)`, `https://`, `http:foo`, `ftp://example.com`, 빈 값(`""`), 공백뿐인 값(`"   "`) → 저장 안 됨, 폼이 다시 보이고 문구가 보이며 제목·메모 입력값이 남아 있다. `https://example.com`, `HTTP://EXAMPLE.COM/a` → 저장됨.
- [ ] `ArticleService`에 URL 검사를 넣고 컨트롤러가 오류 시 폼을 다시 그리게 한다.
- [ ] **403 화면(PR 1에 포함):** 먼저 테스트를 쓴다. 로그아웃 상태에서 `/error`(상태 403 흉내) → 403, "세션이 만료됐어요. 다시 로그인해 주세요", `/login` 링크. 실패를 확인하고 `templates/error/403.html`을 만든다. PR 1에 넣는 이유: CSRF가 PR 1에서 켜지므로 PR 1 배포 직후부터 사용자가 볼 수 있다.
- [ ] 통과 확인.
- [ ] ADR 0003, 0004를 쓴다. `CLAUDE.md` 테스트 규칙을 고친다(`TestAuth.loginAs`, POST에는 `.with(csrf())`).
- [ ] `./gradlew checkstyleMain spotbugsMain test` 통과 확인, 커밋(사용자 승인 후)
- [ ] push하고 PR 1을 만든다(사용자 승인 후). 본문: IDOR 수정 전 실패 기록, 바뀐 로그인 방식, **PR 1 머지 직후 배포**(보안 수정이 목적이라 미루지 않는다), 배포 직후 재로그인 필요, 운영 DB의 빈 URL 글 4건은 그대로 남고 그 글의 수정 화면은 URL을 채워야 저장된다.
- [ ] 사용자가 리뷰하고 머지한다. 에이전트는 머지하지 않는다.

---

# PR 2 로직·구조 (PR 1 머지 후 시작)

## Task 5: 오늘 날짜를 한국 시간으로

**해결하는 문제:** 설계 4장 타임존

- [ ] 실패하는 테스트: 시각을 `2026-11-14T15:30:00Z`(한국 11월 15일 00:30)로 고정하고 `lastPopupDate`가 11월 14일인 사용자 → 팝업이 뜨고 `lastPopupDate`가 11월 15일이 된다.
- [ ] `Clock` Bean(`Asia/Seoul`)을 만들고 `takeTodayPopupWords`가 `LocalDate.now(clock)`을 쓰게 한다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 6: 쿼리 수 측정 + 조회 개선

**해결하는 문제:** 설계 5장

- [ ] 측정 테스트를 먼저 쓴다: 글 10개 × 단어 3개, 팝업이 뜨는 사용자(`lastPopupDate` 비어 있음)로 `GET /articles`를 열고 SQL 실행 수를 센다. 기대값 **5 이하**.
- [ ] 지금 코드에서 실패하는 것을 확인하고 **실제 숫자(수정 전)를 기록한다.**
- [ ] `findMyArticles`를 DB에서 거르는 조회로, 안 외운 단어를 JPQL 한 번으로 바꾼다. 메모리 구현도 맞춘다.
- [ ] 통과 확인하고 수정 후 숫자를 기록한다. 커밋(사용자 승인 후)

## Task 7: 글 삭제 시 단어도 삭제, 팝업 확인은 멱등하게

**해결하는 문제:** 설계 4장 고아 단어, 팝업 토글

- [ ] 실패하는 테스트: 단어 2개가 있는 글 삭제 → 그 글의 단어 0개, 다른 글의 단어는 그대로. 같은 ids로 memorize 두 번 → 여전히 암기 완료.
- [ ] `deleteArticle`이 같은 트랜잭션에서 단어를 먼저 지우게 하고, memorize가 `markMemorized`를 쓰게 한다.
- [ ] 통과 확인, 커밋(사용자 승인 후)

## Task 8: 입력 검증 버그 (카테고리 누락, 가입 폼)

**해결하는 문제:** 설계 4장 Category 누락, 아이디 중복 500, (이번에 발견) 가입 검증 실패 시 직군 목록 사라짐

- [ ] 실패하는 테스트: 카테고리 없이 수정 → 400, DB 카테고리 그대로. 이미 있는 아이디로 가입 → 200, "이미 존재하는 아이디입니다." 보임. 비밀번호를 비우고 가입 → 직군 목록(예: "백엔드 개발자")이 보임.
- [ ] 고친다. 통과 확인, 커밋(사용자 승인 후)

## Task 9: 에러 화면 + 정리

**해결하는 문제:** 설계 6장, 7장

- [ ] 실패하는 테스트: 로그인 사용자가 `GET /articles/999999` → 404, "찾을 수 없는 페이지예요" 보임. 카테고리 없이 수정 → 400, "요청이 올바르지 않아요" 보임. `/error`에 상태 500을 흉내 낸 요청 → "잠시 후 다시 시도해 주세요" 보임.
- [ ] `GlobalExceptionHandler`와 `templates/error/404.html`, `400.html`, `5xx.html`을 만든다. (403은 PR 1에서 만들었다.) 404·400은 핸들러가 직접 화면을 고르고, 500은 `/error`에 상태 500을 흉내 낸 요청으로 화면을 테스트한다.
- [ ] `ArticleService.findByCategory`, `findArticles`, hello 파일 3개를 지운다. Tabler CDN을 `3.51.0`으로 고정한다.
- [ ] v2-0 README에서 b 상태를 갱신한다.
- [ ] `./gradlew checkstyleMain spotbugsMain test` 통과 확인, 커밋(사용자 승인 후)
- [ ] push하고 PR 2를 만든다(사용자 승인 후). 본문: 쿼리 수 전/후.
- [ ] 사용자가 리뷰하고 머지한다.

---

## 주의

- **배포 직후 로그인이 풀린다.** 세션에 담는 객체가 바뀌기 때문이다. 기존 비밀번호는 그대로 쓸 수 있다.
- **운영 DB에 이미 들어간 `javascript:` URL과 고아 단어는 남는다.** 확인 여부는 사람이 정한다(설계 11장).
- **@MockitoBean을 쓰는 Task 5 테스트는 Spring 앱을 하나 더 띄운다.** MySQL 컨테이너도 한 번 더 뜬다. 테스트가 몇 초 느려지는 것은 받아들인다.
