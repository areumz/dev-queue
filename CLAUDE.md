# DevQueue

개발 문서·영상 링크를 카테고리별로 저장하는 읽을거리 큐 + 단어장. 하루 한 번 안 외운 단어 3개를 팝업으로 복습한다.

## 스택

Java 25, Spring Boot 4.1.1, Thymeleaf(SSR), Spring Data JPA, MySQL 8. 인증은 세션 + BCrypt(`spring-security-crypto`).

## 명령어

- 테스트: `./gradlew test` (**Docker가 켜져 있어야 한다.** Testcontainers가 MySQL 8 컨테이너를 띄운다)
- CI와 같은 검사: `./gradlew checkstyleMain spotbugsMain test`
- 실행: `./gradlew bootRun` (`src/main/resources/application-local.properties` 필요, `.example` 파일 참고)
- JDK는 설치하지 않아도 된다. Gradle이 Java 25를 `~/.gradle/jdks`에 받는다.

## 프로필

- 실행: `local,jpa` (`application.properties`에 고정)
- 테스트: `test,jpa` (`@IntegrationTest`가 덮어쓴다. 테스트는 로컬 DB에 절대 붙지 않는다)
- `memory` 프로필의 Memory*Repository는 v1 학습용으로 남아 있다.

## 테스트 작성 규칙

- TDD: 실패하는 테스트 → 실패 확인 → 최소 구현 → 통과 확인.
- 컨트롤러·통합 테스트는 클래스에 `@IntegrationTest @Transactional`을 붙인다. `@Transactional`이 테스트마다 DB를 되돌린다.
- 로그인한 사용자는 `TestSessions.loginAs(user)`로 흉내 낸다(v2-0b에서 Spring Security 방식으로 바뀐다).
- 예시: `src/test/java/com/areumz/devqueue/controller/ArticleControllerTest.java`

## 코드 규칙

- Controller에 비즈니스 로직을 두지 않는다. Service에 둔다.
- Repository는 `repository/` 인터페이스 + `repository/jpa/` 구현 구조를 유지한다.
- 외부 API(LLM, 분류, 크롤링)는 인터페이스 뒤에 숨긴다.
- Checkstyle(`config/checkstyle/checkstyle.xml`)과 SpotBugs가 CI에서 실패하면 머지할 수 없다.
- SpotBugs 경고를 제외할 때는 `config/spotbugs/exclude.xml`에 이유를 주석으로 단다.

## 보안 규칙

- 모든 리소스 접근(조회·수정·삭제)에서 로그인 사용자의 소유권을 검증한다.
- 사용자가 입력한 URL은 http/https만 허용한다.

## 워크플로

- superpowers 사이클: 브레인스토밍 → 설계 승인 → 계획 → 구현 → 머지.
- 사람이 결정하는 지점: 설계 승인, 주요 결정, 커밋 승인, PR 최종 리뷰와 머지.
- 작업은 main에서 분기한 feature 브랜치에서 한다. main에 직접 커밋하지 않는다.
- 사용자가 승인하기 전에는 커밋하지 않는다. 구현 중 커밋 시점은 각 계획 문서의 "공통 제약"을 따른다.
- main은 PR + CI(`code-quality`) 통과로만 머지된다. CI job 이름을 바꾸지 않는다.

## 참고 문서

- v2 설계: `docs/brainstorming/`
- 구현 계획: `docs/plans/`
- 결정 기록(ADR): `docs/decisions/`
- v1 설계 기록: `rpi/article-vocab-tracker/` (수정하지 않는다)
