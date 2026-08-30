# eng.md — Article & Vocabulary Tracker (dev-queue)

> 학습용 프로젝트이므로 "실무 최적 설계"가 아니라 **개념을 명확히 드러내는 설계**를 우선한다. 성능/확장성 최적화는 의도적으로 하지 않는다.

## 1. 스택 & 시작 전 결정

| 항목 | 현재 | 비고 |
|---|---|---|
| Java | 17 | 고정 |
| Spring Boot | 4.1.1 | **결정 필요**: 3.5.x 다운그레이드 권장 (아래 §1.1) |
| 빌드 | Gradle | 고정 |
| 템플릿 | Thymeleaf | `spring-boot-starter-thymeleaf` 기존 존재 |
| 웹 | `spring-boot-starter-webmvc` | Boot 4 전용 스타터명 |
| DB (1단계) | 없음 — 메모리(ArrayList/HashMap) | Repository 인터페이스로 추상화 |
| DB (2단계, 추후) | MySQL + Spring Data JPA | Stage 8에서 도입 |
| 인증 | 직접 구현 (`HttpSession` + `HandlerInterceptor`) | Spring Security 미사용 (이유 §3) |
| 비밀번호 해시 | `org.springframework.security:spring-security-crypto`의 `BCryptPasswordEncoder`만 | Security 스타터 전체 금지 |

### 1.1 Spring Boot 버전 결정 (Stage 0 최우선 작업)
- **권장: 3.5.x로 다운그레이드.** 이유: 세상 대부분의 Spring 입문 튜토리얼이 2.x/3.x 기준이라, 4.1.1을 유지하면 학습 중 "왜 강의랑 다르지"에 반복적으로 부딪힘. 배우려는 개념(DI/MVC/Thymeleaf/세션/JPA)은 3.5와 4.1에서 동일함.
- 4.1.1을 유지하기로 결정한다면: `build.gradle`의 스타터명이 이미 Boot4 네이밍(`spring-boot-starter-webmvc`)이므로 그대로 두되, Stage 0에서 JUnit5/AssertJ가 테스트 스타터 분리 후에도 정상 인식되는지 먼저 확인.
- 이 결정은 코드 작성 전에 사용자가 직접 내려야 하며, PLAN.md Phase 0의 첫 태스크로 배치한다.

## 2. 패키지 구조

```
com.areumz.devqueue
├── domain          # User, Article, Category(enum), Vocabulary — 순수 자바 객체
├── repository       # 인터페이스 + memory 패키지(구현체)
│   └── memory
├── service          # ArticleService, UserService, VocabularyService, AuthService 등
├── controller        # @Controller (뷰 반환), 필요시 DTO(폼 객체)는 controller.form 하위
├── config           # WebMvcConfigurer(인터셉터 등록), PasswordEncoder @Bean
└── DevqueueApplication
```

기존 `HelloController`(튜토리얼용)는 Stage 0~1 완료 후 삭제 대상.

## 3. 인증 설계

**직접 구현(HttpSession + Interceptor)을 선택한 이유**: `spring-boot-starter-security`를 추가하면 전체 필터체인이 자동 설정되어 모든 엔드포인트가 기본 잠기고 폼 POST가 CSRF에 막히는 등, "BCrypt 하나 쓰려다" 예상치 못한 전역 동작 변경을 겪게 됨. 세션 인증의 핵심 개념(로그인=세션 저장, 인가=인터셉터 검사)을 직접 구현하며 이해하는 것이 이번 프로젝트의 목표이므로, Security 필터체인이라는 "블랙박스" 없이 진행한다. Spring Security는 Stage 9(다음 프로젝트)로 이연.

- **회원가입**: `UserService.signup(username, password, nickname)` → 아이디 중복 체크 → `passwordEncoder.encode(rawPassword)` → 저장
- **로그인**: `UserService.login(username, rawPassword)` → `userRepository.findByUsername()` → `passwordEncoder.matches(rawPassword, user.getPassword())` (⚠️ `equals()` 비교 금지 — BCrypt는 매 호출마다 다른 해시 생성) → 성공 시 `HttpSession.setAttribute("loginUser", user)` (또는 userId만 저장 후 매 요청 시 재조회 — 후자를 권장: 세션에 stale 객체가 남는 문제 회피)
- **인가**: `LoginCheckInterceptor implements HandlerInterceptor` — `preHandle`에서 세션에 로그인 정보 없으면 `/login`으로 리다이렉트. `WebMvcConfigurer.addInterceptors()`에 등록하되 `/login`, `/signup`, 정적 리소스는 반드시 `excludePathPatterns`에 포함 (누락 시 로그인 페이지 자체가 인터셉트되어 무한 리다이렉트).
- **로그아웃**: `session.invalidate()`

## 4. 도메인 모델

```java
enum Category { DEV_DOC, AI, JOB, ETC }  // 개발문서 / AI / 채용정보 / 기타

class User {
    Long id;
    String username;
    String password;   // BCrypt 해시
    String nickname;
}

class Article {
    Long id;
    String title;
    String url;
    Category category;
    String memo;
    boolean read;
    Long userId;        // User를 통째로 들고 있지 않고 FK만 (§5 참고)
}

class Vocabulary {
    Long id;
    String word;
    String meaning;
    boolean memorized;
    Long articleId;      // Article이 List<Vocabulary>를 들고 있지 않음
}
```

## 5. Repository 인터페이스 설계 원칙 (Stage 8 성공의 전제조건)

RESEARCH.md §5의 결론을 그대로 계약으로 확정한다 — Stage 1에서 이 규칙을 지켜야 Stage 8(JPA 전환)에서 Controller/Service 코드를 건드리지 않을 수 있다.

| 규칙 | 이유 |
|---|---|
| `T save(T entity)` — id 채워진 객체 반환 (void 아님) | `JpaRepository.save()`와 시그니처 일치 |
| `Optional<T> findById(Long id)` (null 반환 금지) | `JpaRepository`와 동일 |
| `List<T> findByUserId(Long userId)` 형태의 조회 메서드명 사용 | Spring Data 쿼리 메서드 네이밍과 자연스럽게 대응 |
| `void deleteById(Long id)` | `JpaRepository`와 동일 |
| id는 전부 `Long` 통일 | `Map<Long,Article>`을 `int`로 조회 시 오토박싱 불일치로 조용히 null 반환됨 |
| id 채번은 `AtomicLong` 사용, `store.size()+1` 금지 | 삭제 후 재사용 시 중복 id 발생 |
| `Vocabulary`는 `Long articleId`로 참조 (Article이 `List<Vocabulary>` 보유 안 함) | JPA `@ManyToOne` 방향과 일치, cascade/지연로딩 초보자 함정 회피 |
| `clearStore()` 등 테스트 전용 메서드는 **인터페이스가 아니라 memory 구현 클래스에만** 선언 | JPA 구현체엔 대응 메서드가 없음 — 인터페이스에 넣으면 Stage 8에서 깨짐 |

## 6. 컨트롤러 & 라우트

| Method | Path | 설명 |
|---|---|---|
| GET | `/login` | 로그인 폼 |
| POST | `/login` | 로그인 처리 |
| POST | `/logout` | 로그아웃 |
| GET | `/signup` | 회원가입 폼 |
| POST | `/signup` | 회원가입 처리 |
| GET | `/articles` | 리스트 (`?category=DEV_DOC` 등, 파라미터 생략 시 전체) |
| GET | `/articles/new` | 등록 폼 |
| POST | `/articles` | 등록 처리 → redirect `/articles` |
| GET | `/articles/{id}` | 상세 |
| POST | `/articles/{id}/memo` | 메모 수정 |
| POST | `/articles/{id}/read` | 읽음 여부 토글 |
| POST | `/articles/{id}/vocabularies` | 단어 추가 |
| POST | `/vocabularies/{id}/toggle` | 암기 여부 토글 |
| POST | `/vocabularies/{id}/delete` | 단어 삭제 |
| POST | `/vocabularies/memorize` | 팝업에서 체크한 단어들 일괄 암기완료 처리 (`List<Long> ids`) |

모든 `/articles/**`, `/vocabularies/**` 경로는 요청 처리 중 **소유자(userId) 검증**을 반드시 거친다 (US-6, ux.md 엣지케이스: 타 사용자 리소스 접근 시 404/403).

## 7. 단어 암기 팝업 구현 메모

- 팝업 노출 판단은 로그인 성공 처리(POST `/login`) 직후, 세션에 1회성 플래그(`popupShown` 등)를 세팅하는 방식으로 구현 → 리스트 화면에서 플래그 존재 시 팝업 프래그먼트 렌더링 후 플래그 제거
- 대상 단어 조회: 로그인 사용자 소유 Article들의 id 목록 → 해당 Article들에 속한 Vocabulary 중 `memorized=false`인 것 전체 수집 (서비스 레이어에서 ArticleRepository + VocabularyRepository 조합)
- 랜덤 추출: `Collections.shuffle(list)` 후 `new ArrayList<>(list.subList(0, Math.min(3, list.size())))` — `subList`를 그대로 반환하지 말고 복사할 것 (원본 뷰라 이후 수정 시 부작용 가능)
- 체크박스 처리: `@RequestParam(required = false) List<Long> ids` — 아무것도 체크 안 하면 파라미터 자체가 없으므로 null 허용 후 빈 리스트로 취급

## 8. 프론트엔드 (Thymeleaf) 주의사항

- Thymeleaf 3.1+에서 `#session`/`#httpSession` 등 유틸리티 객체 제거됨 — 세션 값은 `@ModelAttribute` 메서드나 인터셉터에서 `Model`에 담아 뷰로 전달할 것 (오래된 예제 코드 그대로 쓰면 파싱 에러)
- "전체" 카테고리 탭: `category` 쿼리 파라미터를 아예 붙이지 않는 링크로 구현 (빈 문자열 전달 시 enum 변환 실패로 400)
- 폼 바인딩은 `th:object`+`th:field` 사용, 등록/수정 후에는 POST-Redirect-GET 패턴 준수 (새로고침 시 중복 제출 방지)

## 9. 데이터 계층 전환 계획 (Stage 8, 이번 단계 범위 밖이지만 설계에 미리 반영)

- `spring-boot-starter-data-jpa` + MySQL 드라이버 추가
- 도메인 클래스에 `@Entity`/`@Id`/`@GeneratedValue`/`@Enumerated(EnumType.STRING)`/`@ManyToOne(fetch = LAZY)` 부착
- `SpringDataJpaArticleRepository extends JpaRepository<Article, Long>`를 만들고, 기존 `ArticleRepository` 인터페이스를 구현하는 어댑터로 감싸거나, `ArticleRepository` 자체를 `JpaRepository`를 확장하도록 전환
- `@Profile("memory")` / `@Profile("jpa")`로 구현체를 프로파일 기반 전환
- **성공 판정 기준**: 이 전환 과정에서 Controller/Service 코드가 한 줄도 바뀌지 않아야 함 (§5 인터페이스 설계 원칙이 지켜졌는지의 최종 검증)

## 10. 테스트 전략

- Stage 1: Spring 컨테이너 없이 순수 JUnit5로 memory Repository 테스트 (`save`/`findById`/`findAll`/`deleteById`) → 이후 `@SpringBootTest`로 DI 조립 확인
- Stage 2~6: 필요 시 `@WebMvcTest` + `MockMvc`로 컨트롤러 단위 테스트 (선택, Stage 7로 미뤄도 무방)
- Stage 8: Stage 1에서 작성한 Repository 계약 테스트를 JPA 구현체에 그대로 재실행 — 인터페이스 설계가 올바랐는지 확인하는 가장 확실한 방법

## 11. 알려진 리스크 & 함정 (RESEARCH.md §7 원문 반영)

| # | 함정 | 대응 |
|---|---|---|
| 1 | BCrypt 해시는 `equals()` 비교 불가 | 항상 `encoder.matches(원문, 해시)` 사용 |
| 2 | BCrypt만 쓰려다 Security 스타터 전체 도입 | `spring-security-crypto` 단일 의존성만 추가 |
| 3 | 메모리 Repository가 객체 참조를 그대로 반환 → `save()` 없이도 수정이 반영된 것처럼 보임 | "수정 후 항상 명시적으로 `save()` 호출" 규칙을 스스로 지킬 것 — Stage 8에서 이 습관이 없으면 트랜잭션 문제로 오인하기 쉬움 |
| 4 | 인터셉터 `excludePathPatterns`에 로그인/회원가입 경로 누락 | 무한 리다이렉트 발생 — 반드시 제외 목록 확인 |
| 5 | 체크 안 한 체크박스는 폼 전송 자체가 안 됨 | `@RequestParam(required=false)` + null 처리 |
| 6 | Thymeleaf 3.1+ `#session` 제거 | `Model`/`@ModelAttribute`로 세션 값 전달 |
| 7 | `subList(0,3)`이 size<3일 때 예외 | `Math.min(3, size)` 사용, 결과는 `new ArrayList<>()`로 복사 |

## 12. 이번 단계 범위 밖 (기술적으로도 제외)

- Spring Security 필터 체인
- 페이지네이션/검색 쿼리 최적화
- 캐싱, 비동기 처리
- REST API (전 화면 서버 렌더링 MVC로 통일 — API 분리는 학습 목표 밖)
