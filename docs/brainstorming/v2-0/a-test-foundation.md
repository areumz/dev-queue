# v2-0a 테스트 기반 설계

> 상위 문서: [v2-0 개요](README.md)
> 목표: 외부 DB 없이 `./gradlew test`가 어디서든 돌고, CI가 테스트 실패한 PR을 막는다.

---

## 1. 현재 문제와 해결 방법

| # | 문제 | 왜 문제인가 | 해결 |
|---|---|---|---|
| 1 | **로컬에서 빌드가 안 된다** | `build.gradle`은 Java 17을 요구하는데 개발 Mac에는 Java 26만 있다. Gradle이 즉시 멈춘다. 서브에이전트가 TDD로 테스트를 돌릴 수 없다. | `settings.gradle`에 `foojay-resolver` 플러그인을 추가한다. 필요한 JDK가 없으면 Gradle이 `~/.gradle/jdks`에 받아서 쓴다. 시스템 Java는 바뀌지 않는다. |
| 2 | **Java 17은 두 단계 전 LTS다** | 최신 LTS는 Java 25(2025년 9월)다. 가상 스레드처럼 이후 외부 API 호출에 유용한 기능을 못 쓴다. | toolchain을 25로 올린다. CI의 JDK도 25로 맞춘다. |
| 3 | **SpotBugs 플러그인이 Java 25를 못 읽는다** | 현재 6.0.7이 쓰는 SpotBugs는 Java 25 바이트코드(컴파일된 `.class` 파일)를 분석하지 못할 가능성이 높다. | Java 25를 지원하는 최신 6.x로 올린다. 정확한 버전은 계획 단계에서 고정한다. |
| 4 | **테스트가 MySQL 없이 못 돈다** | `contextLoads`와 `ArticleServiceTest`는 `@SpringBootTest`라서 로컬 MySQL에 붙으려 한다. CI에는 MySQL이 없다. | **Testcontainers**로 테스트 시작 시 MySQL 8 컨테이너를 띄우고 끝나면 지운다. 운영과 같은 DB라서 MySQL 전용 문법도 검증된다. |
| 5 | **테스트 설정이 운영 설정과 섞인다** | `application.properties`가 `local,jpa` 프로필을 켠다. 테스트도 로컬 DB 설정과 `ddl-auto=update`를 그대로 따라간다. | `application-test.properties`를 따로 두고, 테스트는 `test,jpa` 프로필로만 돈다. 테이블은 `create-drop`(시작할 때 만들고 끝나면 지움)으로 매번 새로 만든다. |
| 6 | **CI가 테스트를 안 돌린다** | `ci.yml`은 Checkstyle과 SpotBugs만 실행한다. 테스트가 깨져도 머지된다. | `./gradlew test` 단계를 추가한다. |
| 7 | **SpotBugs 실패가 무시된다** | `ignoreFailures = true`라서 경고가 있어도 CI가 초록불이다. 도구를 돌리는 의미가 없다. | `ignoreFailures`를 끈다. 기존 경고 처리 방식은 3장. |
| 8 | **CI가 실패해도 머지할 수 있다** | GitHub 저장소에 "체크 통과 필수" 설정이 없다. | main 브랜치 보호 규칙에 CI 체크(`code-quality`)를 필수로 건다. 저장소 설정이라 사람이 직접 한다(5장). |
| 9 | **컨트롤러를 테스트할 틀이 없다** | b의 IDOR 재현 테스트는 "로그인한 A가 B의 글에 요청을 보낸다"를 흉내 내야 한다. 지금은 그 방법이 없다. | MockMvc(서버를 띄우지 않고 가짜 HTTP 요청을 컨트롤러에 보내는 도구) 기반 통합 테스트 틀과 로그인 흉내 도우미를 만든다(2장). |
| 10 | **쓰지 않는 `h2.console` 설정** | H2 의존성이 없는데 설정만 남아 있다. 읽는 사람을 헷갈리게 한다. | 한 줄 삭제. |

---

## 2. 테스트 틀 구조

### 2-1. 구성 요소

**`TestcontainersConfiguration`** (테스트 전용 설정 클래스)
- MySQL 8 컨테이너를 Bean(Spring이 관리하는 객체)으로 등록한다.
- `@ServiceConnection`을 붙인다. Spring Boot가 컨테이너의 주소, 포트, 계정을 읽어서 DB 연결 설정을 자동으로 채운다. URL을 직접 적지 않아도 된다.
- 포트는 비어 있는 아무 포트를 쓴다. 로컬에서 다른 MySQL이 3306을 쓰고 있어도 충돌하지 않는다.

**`@IntegrationTest`** (직접 만드는 어노테이션 하나)
- 아래 4개를 한 번에 붙이는 묶음이다. 통합 테스트 클래스마다 4줄을 반복하지 않으려고 만든다.
  - `@SpringBootTest`: 앱 전체를 띄운다
  - `@AutoConfigureMockMvc`: MockMvc를 쓸 수 있게 한다
  - `@ActiveProfiles({"test", "jpa"})`: 테스트 설정과 JPA Repository만 켠다. `local` 프로필은 켜지지 않는다.
  - `@Import(TestcontainersConfiguration.class)`: 위 MySQL 컨테이너를 붙인다
- 이 어노테이션이 붙은 테스트 클래스들은 Spring이 같은 앱 상태를 재사용한다(테스트 컨텍스트 캐시). 그래서 컨테이너도 한 번만 뜬다.

**`TestSessions.loginAs(User user)`** (로그인 흉내 도우미)
- 지금 로그인 방식을 그대로 흉내 낸다. 세션에 `loginUser`를 넣은 `MockHttpSession`을 돌려준다.
- **임시 도우미다.** b에서 Spring Security를 넣으면 Security가 제공하는 방식으로 바꾼다. 그래서 일부러 한 곳에만 둔다. 바꿀 때 이 파일만 고치면 된다.

### 2-2. 테스트 데이터 정리

- 통합 테스트 클래스에 `@Transactional`을 붙인다. 테스트 하나가 끝날 때마다 DB 변경이 되돌려진다(롤백). 테스트끼리 데이터가 섞이지 않는다.
- MockMvc는 테스트와 같은 스레드에서 요청을 처리하기 때문에 이 롤백이 컨트롤러 안의 저장까지 덮는다.

### 2-3. 이 틀이 동작한다는 증거 (a에서 쓰는 테스트)

`ArticleControllerTest`에 아래 테스트를 둔다. b의 IDOR 테스트가 그대로 따라 쓸 수 있는 모양이다.

1. **비로그인 사용자가 `GET /articles` 요청** → `/login`으로 리다이렉트된다
2. **로그인한 사용자가 `GET /articles` 요청** → 200 응답, 내가 저장한 글 제목이 화면에 있다
   - 사용자와 글은 테스트 안에서 실제 Repository로 저장한다. Testcontainers MySQL에 진짜로 들어간다.

기존 테스트는 이렇게 맞춘다.
- `DevqueueApplicationTests.contextLoads`: `@IntegrationTest`로 바꾼다. 외부 DB 없이 통과한다.
- `ArticleServiceTest`: 같은 방식으로 바꾼다.
- `MemoryArticleRepositoryTest`, `MemoryUserRepositoryTest`: Spring 없이 도는 단위 테스트라 그대로 둔다.

---

## 3. SpotBugs 기존 경고 처리 규칙

현재 경고 개수는 모른다. 로컬 빌드가 안 돼서 측정하지 못했다(1장 #1). 구현 첫 단계에서 측정해서 PR에 숫자로 남긴다.

- **기계적으로 고칠 수 있는 경고**(예: getter가 내부 리스트를 그대로 내보냄): a에서 고친다.
- **b의 보안·로직 수정과 겹치는 경고**: 제외 파일 `config/spotbugs/exclude.xml`에 넣고, 항목마다 "b에서 해결" 주석을 단다. b가 끝날 때 해당 항목을 지운다.
- **경고가 20개를 넘으면** 구현을 멈추고 사람에게 묻는다. a의 범위가 커지는 신호이기 때문이다.

---

## 4. CI 변경

`.github/workflows/ci.yml`
- JDK 17 → 25
- `setup-java`에 `cache: gradle`을 추가한다. 의존성을 매번 새로 받지 않아서 CI가 빨라진다.
- 단계 순서: Checkstyle → SpotBugs → **Test(신규)**
- GitHub Actions 우분투 러너에는 Docker가 기본으로 있어서 Testcontainers를 별도 설정 없이 쓸 수 있다.

---

## 5. 사람이 직접 하는 일

- **main 브랜치 보호 규칙 설정**: Settings → Rules → Rulesets → New branch ruleset
  - Ruleset Name: `main-protection`, Enforcement status: **Active**
  - Bypass list: 비워 둔다. 저장소 주인도 CI 실패 PR을 머지하지 못하게 하려는 것이다.
  - Target branches: Add target → **Include default branch**
  - 체크하는 규칙 3개
    - **Restrict deletions**: main 삭제 금지
    - **Require a pull request before merging**: main에 직접 push 금지. **Required approvals는 0**으로 둔다. 혼자 개발하면 자기 PR을 승인할 수 없어서, 1 이상이면 머지가 영원히 막힌다.
    - **Require status checks to pass**: Add checks에서 `code-quality`를 추가한다. 기존 PR에서 CI가 돌았으니 목록에 뜬다. 안 뜨면 이 PR을 올리고 CI가 한 번 돈 뒤에 추가한다.
  - **Block force pushes**는 기본으로 체크돼 있으면 그대로 둔다.
  - 나머지는 체크하지 않는다. 특히 **Require linear history**는 끈다. 지금 PR을 머지 커밋 방식으로 합치고 있어서 켜면 머지가 막힌다.
  - CI 자체가 고장 나서 급하게 머지해야 하면, ruleset을 잠깐 Disabled로 바꿨다가 되돌린다.
- **PR 최종 리뷰와 머지**

---

## 6. 문서 작업 (a에 포함)

- **`CLAUDE.md` 최소 버전 작성**: 서브에이전트는 매번 대화 내용 없이 시작한다. 빌드·테스트 명령(`./gradlew test`, Docker 필요), 프로필 구조, `@IntegrationTest` 사용법, 보안 규칙(모든 리소스 접근에서 소유권 검증, URL은 http/https만), 사람이 승인하는 지점을 적는다. 브리프 5-3장의 나머지 항목(URL 정규화 규칙 등)은 해당 사이클에서 추가한다.
- **ADR 2개**: `docs/decisions/0001-testcontainers.md`(H2 대신 Testcontainers를 고른 이유), `0002-java-25.md`(17에서 25로 올린 이유).

---

## 7. 범위 밖 (다른 사이클에서)

- 운영 설정 정리(`ddl-auto=update`, `show-sql`) → b. `ddl-auto=update`를 대체하려면 DB 변경 이력 도구(Flyway 등) 도입 여부를 함께 정해야 해서, a에서 다루기엔 크다.
- IDOR 재현 테스트와 모든 버그·보안 수정 → b
- `.claude/agents/` 정리, README의 v1/v2 구분 → a 머지 후 별도로 정한다. 테스트 기반과 관계가 없다.

---

## 8. 위험

- **기존 Docker 설정 때문에 MySQL 이미지를 못 받을 수 있다.** Docker Hub 접근이 막히면 첫 `./gradlew test`에서 바로 드러난다.
- **로컬 테스트에는 Docker가 켜져 있어야 한다.** 꺼져 있으면 통합 테스트가 실패한다. `CLAUDE.md`에 적는다.
- **Spring Boot 4에서 Testcontainers 관련 라이브러리 이름이 바뀌었을 수 있다**(Testcontainers 2.x). 계획 단계에서 실제 이름을 확인하고 고정한다.
- **Java 25 전환 시 Checkstyle·SpotBugs가 새 버전과 안 맞을 수 있다.** 구현 첫 단계에서 빌드가 통과하는지부터 확인한다.

---

## 9. 완료 기준

1. 이 Mac에서 Java 17/25를 따로 설치하지 않고 `./gradlew test`가 통과한다(Docker만 켜져 있으면 됨).
2. `contextLoads`가 외부 MySQL 없이 통과한다.
3. `ArticleControllerTest`의 테스트 2개가 통과한다(비로그인 리다이렉트, 로그인 후 목록).
4. CI에서 Checkstyle, SpotBugs(`ignoreFailures` 꺼짐), 테스트가 모두 돈다.
5. 일부러 실패하는 테스트를 넣은 PR은 CI가 빨간불이 되고 머지 버튼이 막힌다(브랜치 보호 설정 후 확인).
6. SpotBugs 기존 경고 수(전/후)가 PR 본문에 숫자로 남아 있다.
7. `CLAUDE.md`와 ADR 2개가 있다.
