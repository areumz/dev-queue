# v2-0a 테스트 기반 구현 계획

> 설계 문서: [docs/brainstorming/v2-0/a-test-foundation.md](../../brainstorming/v2-0/a-test-foundation.md)

**목표:** 외부 DB 없이 `./gradlew test`가 어디서든 돌고, CI가 테스트 실패한 PR을 막는다.

**방식:** Gradle toolchain을 Java 25로 올리고 foojay 플러그인으로 JDK를 자동으로 받는다. 통합 테스트는 `@IntegrationTest` 어노테이션 하나로 Spring 전체, MockMvc, Testcontainers MySQL을 붙인다. CI는 Checkstyle → SpotBugs(실패 시 중단) → 테스트 순서로 돈다.

**기술:** Java 25, Gradle 9.5.1, Spring Boot 4.1.1, Testcontainers 2.x(Spring Boot가 버전 관리), MySQL 8.0 이미지, SpotBugs Gradle plugin 6.5.13, GitHub Actions

## 공통 제약

- Java toolchain은 25. 시스템 JDK를 설치하지 않는다. foojay-resolver-convention 1.0.0이 받는다.
- SpotBugs Gradle plugin은 6.5.13, `ignoreFailures = false`.
- Checkstyle 버전(10.12.5)은 바꾸지 않는다.
- 테스트 DB는 Testcontainers MySQL(`mysql:8.0`)만 쓴다. H2를 추가하지 않는다.
- 테스트 프로필은 `test,jpa`. 테스트에서 `local` 프로필을 켜지 않는다.
- 운영 설정(`ddl-auto=update`, `show-sql`)은 건드리지 않는다. `spring.h2.console.enabled` 한 줄만 지운다.
- git worktree를 쓰지 않는다. 코드 포함 계획과 `application-local.properties`가 gitignore라서 worktree에 없다.

## 바뀌는 파일

| 파일                                                         | 작업 | 역할                                       |
| ------------------------------------------------------------ | ---- | ------------------------------------------ |
| `settings.gradle`                                            | 수정 | foojay 플러그인                            |
| `build.gradle`                                               | 수정 | Java 25, SpotBugs 버전·설정, 테스트 의존성 |
| `src/main/resources/application.properties`                  | 수정 | `h2.console` 줄 삭제                       |
| `config/spotbugs/exclude.xml`                                | 생성 | SpotBugs 제외 목록(이유 주석 포함)         |
| `src/main/java/.../domain/User.java`                         | 수정 | `getRoleDetail()` 추가                     |
| `src/test/resources/application-test.properties`             | 생성 | 테스트 전용 설정                           |
| `src/test/java/.../support/TestcontainersConfiguration.java` | 생성 | MySQL 컨테이너 등록                        |
| `src/test/java/.../support/IntegrationTest.java`             | 생성 | 통합 테스트 어노테이션 묶음                |
| `src/test/java/.../support/TestSessions.java`                | 생성 | 로그인 흉내 도우미(임시, v2-0b에서 교체)   |
| `src/test/java/.../controller/ArticleControllerTest.java`    | 생성 | 틀이 동작한다는 증거 테스트 2개            |
| `src/test/java/.../DevqueueApplicationTests.java`            | 수정 | `@IntegrationTest`로 전환                  |
| `src/test/java/.../service/ArticleServiceTest.java`          | 수정 | `@IntegrationTest`로 전환                  |
| `.github/workflows/ci.yml`                                   | 수정 | JDK 25, Gradle 캐시, 테스트 단계           |
| `CLAUDE.md`                                                  | 생성 | 서브에이전트용 프로젝트 맥락               |
| `docs/decisions/0001-testcontainers.md`                      | 생성 | ADR                                        |
| `docs/decisions/0002-java-25.md`                             | 생성 | ADR                                        |
| `docs/brainstorming/v2-0/README.md`                          | 수정 | a 상태 갱신                                |

---

## Task 1: Java 25로 빌드되게 하기

**해결하는 문제:** 설계 1장 #1(로컬 빌드 불가), #2(Java 17), #10(`h2.console`)

- [ ] 지금 `./gradlew compileJava`가 "Java 17을 찾을 수 없다"로 실패하는 것을 확인한다.
- [ ] `settings.gradle`에 foojay-resolver-convention 1.0.0 플러그인을 추가한다.
- [ ] `build.gradle`의 toolchain을 25로, SpotBugs 플러그인을 6.5.13으로 올린다. 6.0.7은 Java 25로 컴파일된 파일을 분석하지 못한다.
- [ ] `application.properties`에서 `spring.h2.console.enabled=true`를 지운다.
- [ ] `./gradlew compileJava checkstyleMain`과 Spring 없이 도는 Memory 테스트 7개가 통과하는지 확인한다. `@SpringBootTest` 테스트는 Task 3 전까지 MySQL이 필요해서 돌리지 않는다.
- [ ] 커밋(Task 1~5 전체 승인 후, Task별로)

## Task 2: SpotBugs 실패를 무시하지 않기

**해결하는 문제:** 설계 1장 #7

- [ ] `ignoreFailures = false`로 바꾸고 실패하는 것을 확인한다. 경고 수(예상 7개)를 PR 본문용으로 기록한다. 20개를 넘으면 멈추고 사람에게 묻는다.
- [ ] 경고 7개를 아래처럼 처리한다.
  - **Spring 오탐 5개**(`EI_EXPOSE_REP2`): 생성자로 받은 Service·Repository를 필드에 저장하는 건 Spring의 정상적인 방식인데, SpotBugs는 이걸 "내부 객체를 밖에 노출한다"고 오해한다. 제외 목록에 넣고 이유를 주석으로 단다.
  - **v2-0b에서 해결할 1개**(세션에 `User` 엔티티 저장): 제외 목록에 넣고 "v2-0b에서 제거" 주석을 단다.
  - **바로 고칠 1개**(`User.roleDetail`을 읽는 곳이 없음): getter를 추가한다. v2-0c 마이페이지에서도 쓸 값이다.
- [ ] `./gradlew spotbugsMain`이 경고 0개로 통과하는지 확인한다.
- [ ] 커밋(Task 1~5 전체 승인 후, Task별로)

## Task 3: Testcontainers 통합 테스트 틀

**해결하는 문제:** 설계 1장 #4(MySQL 없이 테스트 불가), #5(테스트·운영 설정 섞임), #9(컨트롤러 테스트 틀 없음)

- [ ] **실패하는 테스트를 먼저 쓴다.** `ArticleControllerTest`에 2개를 쓴다.
  - 로그인 안 한 사용자가 `/articles`에 접근하면 `/login`으로 리다이렉트된다.
  - 로그인한 사용자가 `/articles`에 접근하면 자기 글 제목이 보인다.
- [ ] 아직 `@IntegrationTest`가 없어서 컴파일 에러로 실패하는 것을 확인한다.
- [ ] Testcontainers 테스트 의존성 3개를 추가한다(버전은 Spring Boot가 관리).
- [ ] `application-test.properties`를 만든다. 테이블은 `create-drop`. DB 주소는 적지 않는다(컨테이너 정보로 자동으로 채워진다).
- [ ] `TestcontainersConfiguration`, `@IntegrationTest`, `TestSessions`를 만든다.
- [ ] 새 테스트 2개가 통과하는지 확인한다. 첫 실행 때 MySQL 이미지(약 600MB)를 받는다. 이미지를 못 받으면 멈추고 사람에게 알린다.
- [ ] 기존 `DevqueueApplicationTests`, `ArticleServiceTest`를 `@IntegrationTest`로 바꾼다.
- [ ] `./gradlew test` 전체가 통과하는지 확인한다(5개 클래스, 11개 테스트).
- [ ] 테스트용 컨테이너가 남아 있지 않은지 확인한다.
- [ ] 커밋(Task 1~5 전체 승인 후, Task별로)

**v2-0b에 넘기는 것:** `@IntegrationTest`, `TestSessions.loginAs(user)`, `ArticleControllerTest`의 작성 패턴

## Task 4: CI에서 테스트 돌리기

**해결하는 문제:** 설계 1장 #6

- [ ] `ci.yml`의 JDK를 25로 바꾸고 Gradle 캐시를 켠다.
- [ ] SpotBugs 다음에 테스트 단계를 추가한다.
- [ ] **job 이름 `code-quality`는 바꾸지 않는다.** main 브랜치 보호 규칙의 필수 체크가 이 이름이다.
- [ ] CI와 같은 명령(`./gradlew checkstyleMain spotbugsMain test`)을 로컬에서 실행해 통과를 확인한다.
- [ ] 커밋(Task 1~5 전체 승인 후, Task별로)

## Task 5: CLAUDE.md와 ADR

**해결하는 문제:** 설계 6장

- [ ] `CLAUDE.md`를 만든다. 스택, 명령어(Docker 필요), 프로필 구조, 테스트 작성 규칙, 코드·보안 규칙, 진행 규칙(main에서 분기한 feature 브랜치에서 작업, 사용자 승인 전 커밋 금지), 사람이 결정하는 지점, 참고 문서 위치를 적는다.
- [ ] ADR `0001-testcontainers.md`: H2 대신 Testcontainers를 고른 이유
- [ ] ADR `0002-java-25.md`: 17에서 25로 올린 이유. 결과에 "배포 서버(EC2)의 Java도 25로 맞춰야 한다"를 적는다.
- [ ] v2-0 README 표의 a 상태를 "구현 완료, PR 리뷰 대기"로 바꾼다.
- [ ] 커밋(Task 1~5 전체 승인 후, Task별로)

## Task 6: PR과 "실패하면 머지 불가" 확인 (사람과 함께)

**해결하는 문제:** 설계 9장 완료 기준 #5, #6

- [ ] push하고 PR을 만든다(사용자 승인 후). PR 본문에 SpotBugs 경고 수(7 → 0)와 테스트 수를 숫자로 적는다.
- [ ] CI `code-quality`가 초록불인지 확인한다.
- [ ] 일부러 실패하는 테스트 커밋을 push해서 CI가 빨간불이 되고 머지 버튼이 막히는지 사용자가 확인한다(사용자 승인 후).
- [ ] 그 커밋을 revert해서 다시 초록불로 만든다.
- [ ] 사용자가 최종 리뷰하고 머지한다. 에이전트는 머지하지 않는다.

---

## 주의

- **배포 서버의 Java 버전:** 이 PR이 머지되면 앱은 Java 25가 있어야 실행된다. 다음 배포 전에 EC2의 Java를 25로 맞춰야 한다. 이번 계획 범위에는 배포가 없다.
