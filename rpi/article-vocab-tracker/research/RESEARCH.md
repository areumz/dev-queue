# RESEARCH.md — Article & Vocabulary Tracker (dev-queue)

> 이 문서는 "실무 최적 설계"를 위한 기술 검토가 아니라, **Spring Boot를 처음 배우는 사람이 이 기능들을 통해 어떤 개념을 어떤 순서로 익히면 좋을지**를 정리한 학습 로드맵입니다.

## 1. 한 줄 요약

로그인한 사용자가 개발 아티클/영상 링크를 카테고리별로 저장하고, 아티클마다 모르는 단어를 등록해 로그인 시 랜덤 3개씩 복습하는 개인용 큐 + 단어장 앱. **개념 난이도상 무리 없는 범위이며, 학습 프로젝트로 진행(GO)을 권장합니다.** 다만 시작 전에 결정해야 할 것이 하나 있습니다 — Spring Boot 4.1.1 vs 3.5.x 버전 선택 (§6 참고).

## 2. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 기능명 | Article & Vocabulary Tracker (dev-queue) |
| 유형 | 신규 풀스택 CRUD 웹앱 (서버 렌더링 MVC) — 그린필드 |
| 현재 코드베이스 | Spring Boot 4.1.1 / Java 17 / Gradle. 도메인 코드 없음 (튜토리얼용 `HelloController`만 존재) |
| 의존성 현황 | `spring-boot-starter-thymeleaf`, `spring-boot-starter-webmvc` (+테스트 스타터). JPA/DB/Security 없음 |
| 참고 자료 | `mockup/login.png`, `list.png`, `note.png`, `form.png`, `words-popup.png` (회원가입 목업은 없음) |

## 3. 요구사항 요약

**엔티티 관계**
- `User` (id, password-BCrypt, nickname) — `Article`의 작성자 (1:N)
- `Article` (title, url, category-Enum, memo, 읽음여부) — `Vocabulary`와 1:N
- `Vocabulary` (word, meaning, 암기여부) — `Article`에 N:1로 속함
- `Category` Enum: 개발문서 / AI / 채용정보 / 기타

**핵심 동작**
- 세션 기반 로그인/회원가입, 비밀번호 BCrypt 암호화
- 카테고리 필터가 있는 리스트, 메모+단어 관리가 있는 상세 화면, 새 링크 등록 폼
- 로그인 시 암기여부=false인 단어 중 랜덤 3개 팝업 → 체크한 것만 암기완료 처리
- 데이터 계층: 1단계 Repository 인터페이스 + 메모리 구현체 → 2단계(추후) JPA/MySQL로 교체

**아직 정해지지 않은 세부사항 (진행에 방해되지는 않지만, 코딩하면서 직접 결정할 것들)**
1. 세션 인증을 직접(`HttpSession`+인터셉터) 만들지, Spring Security를 쓸지 → **직접 만드는 것을 권장** (이유는 §6 참고)
2. 아이디/닉네임 중복 검사 여부와 유효성 검사 규칙
3. 읽음여부는 체크박스로 수동 토글인지, 상세 진입 시 자동 처리인지
4. 안 외운 단어가 3개 미만/0개일 때 팝업 처리 방식
5. 팝업 대상 단어가 "로그인한 사용자 소유 아티클"로 한정되는지 (소유 모델상 그래야 자연스러움)
6. 다른 사용자의 글을 볼 수 있는지 여부 (기본적으로 본인 것만 접근 가능하다고 가정)

## 4. 개념 학습 로드맵 (핵심)

각 단계는 **눈에 보이는 결과물이 나오는 지점**에서 끝나도록 나눴습니다. 뒤 단계는 앞 단계의 결과물 위에 쌓입니다.

### Stage 0 — 스캐폴딩 & 첫 화면 렌더링 (Simple)
- **개념**: `@Controller`, `Model`, 뷰 리졸빙, `th:each`/`th:text`, 정적 리소스
- **할 일**: 패키지 구조(`domain`/`repository`/`service`/`controller`/`config`) 잡기. `list.png` 목업을 컨트롤러에 하드코딩한 가짜 데이터로 렌더링 (Repository도, 도메인 클래스도 아직 없음)
- **완료 기준**: 가짜 데이터 3줄이 목업처럼 화면에 뜬다

### Stage 1 — 도메인 + 메모리 Repository (웹 계층 없이) (Simple→Medium)
- **개념**: Enum 필드, 인터페이스, `Optional<T>`, `ConcurrentHashMap`/`AtomicLong`, JUnit5 → 그 다음 DI, 싱글톤 스코프, 생성자 주입
- **할 일**: `Article`, `Category` enum, `ArticleRepository` 인터페이스, `MemoryArticleRepository` 구현. **Spring 없이 순수 JUnit 테스트**로 검증 → 그 다음에 `@Repository`/`@Service` 붙여서 `@SpringBootTest`로 컨테이너 조립 확인
- 왜 웹 없이 먼저 하냐면: "자바 객체 모델링"과 "Spring의 마법"을 분리해서 각각 이해하기 위함
- **완료 기준**: save/findById/findAll/delete 테스트 통과 + Service가 정상 주입됨

### Stage 2 — 아티클 CRUD (웹) (Medium)
- **개념**: `@PostMapping`, `@ModelAttribute` 바인딩, `th:object`/`th:field`, `@PathVariable`, POST 후 리다이렉트 패턴, 폼 DTO vs 도메인 객체
- **할 일**: Stage 0의 가짜 데이터를 실제 Repository로 교체. 새 링크 등록(GET 폼→POST 저장→redirect), 상세 조회, 메모 수정, 읽음/안읽음 토글
- **완료 기준**: 등록→조회→수정 전체 흐름이 재시작 전까지 유지됨
### Stage 3 — 카테고리 필터 (Simple)
- **개념**: Enum 타입의 `@RequestParam`, 활성 탭 표시(`th:classappend`), 필터 로직을 뷰가 아닌 서비스에 두기
- **주의**: "전체" 탭은 `category` 파라미터를 아예 생략해야 함 (빈 문자열을 보내면 enum 변환 실패로 400 에러)

### Stage 4 — 인증 (Medium)
- **개념**: 세션(`HttpSession`), `@Bean`으로 등록하는 `PasswordEncoder`, BCrypt `encode`/`matches`, `HandlerInterceptor`+`WebMvcConfigurer`, `@Valid`+`BindingResult`, 전역 에러, 플래시 메시지
- **할 일**: `User` 도메인+Repository, 회원가입(중복 아이디/비밀번호 확인 검증), 로그인→세션 저장, 로그아웃→세션 무효화, 로그인/회원가입 제외 전체 페이지 보호하는 인터셉터
- **이어서(4b)**: `Article`에 `userId` 추가 → 리스트/상세를 로그인 사용자 것만 보이게 리팩터링. 아티클 CRUD를 먼저 만들고 나중에 소유권을 붙이는 순서를 권장 (더 빨리 눈에 보이는 결과가 나오고, 메모리 단계라 리팩터링 비용도 낮음)
- **완료 기준**: 로그아웃 상태로 접근 시 로그인으로 튕김, 계정 두 개로 서로 다른 리스트가 보임

### Stage 5 — 단어(Vocabulary) (Medium)
- **개념**: FK 필드로 1:N 표현(`articleId`), `findByArticleId`, 서비스 레이어에서 여러 Repository 조합
- **할 일**: `Vocabulary` 도메인+Repository, 상세 화면에서 단어 추가/삭제, 암기여부 토글

### Stage 6 — 단어 암기 팝업 (Medium) — 이 프로젝트의 클라이맥스
- **개념**: Thymeleaf 프래그먼트, `Collections.shuffle`+안전한 `subList`, `@RequestParam(required=false) List<Long>`, 세션의 1회성 플래그, 빈 상태 처리
- **할 일**: 로그인 성공 시 → 해당 사용자 소유 아티클들의 안 외운 단어 전체 수집 → shuffle → 최대 3개 추출 → `words-popup.png` 형태로 렌더링 → 체크한 id들만 POST로 받아 암기완료 처리 → 새로고침 시 재노출 안 되도록 세션 플래그 제거
- 이 단계가 처음으로 "두 Repository 조합 + 세션 생명주기 + 알고리즘"을 동시에 요구하는 지점입니다. 앞 단계보다 한 단계 어려워지는 게 정상입니다.

### Stage 7 — 다듬기 (Simple→Medium, 생략 가능)
- `@ControllerAdvice`+에러 페이지, `messages.properties`, 목업에 맞춘 CSS, `@WebMvcTest`/`MockMvc` 테스트. 스타일링이 학습 흐름을 막지 않도록 의도적으로 맨 뒤에 배치

### Stage 8 — JPA + MySQL 전환 (Medium→Complex) — 명확한 경계
- **개념**: `@Entity`/`@Id`/`@GeneratedValue`/`@Enumerated(STRING)`/`@ManyToOne(LAZY)`, `JpaRepository<T,Long>`, `@Transactional`, 영속성 컨텍스트/dirty checking
- **할 일**: `spring-boot-starter-data-jpa`+MySQL 드라이버 추가 → 엔티티 어노테이션 → `SpringDataJpaArticleRepository extends JpaRepository`를 자신의 `ArticleRepository` 인터페이스로 감싸는 어댑터 작성 → `@Profile("memory")`/`@Profile("jpa")`로 구현체 전환
- **성공 기준**: Repository 인터페이스를 Stage 1에서 잘 설계했다면, **Controller/Service 코드는 한 줄도 안 바뀌어야 함**. 이 결과 자체가 이번 프로젝트에서 배우는 가장 중요한 교훈입니다 (§5의 인터페이스 설계 원칙 참고)

### Stage 9 (선택, 다음 프로젝트) — Spring Security
- 직접 만든 세션 인증을 Spring Security의 필터 체인으로 교체. 개념 도약폭이 커서 이번 프로젝트에는 포함하지 않는 것을 권장

## 5. Repository 인터페이스를 지금 잘 설계해야 하는 이유

Stage 8의 성공 여부는 Stage 1에서 인터페이스를 어떻게 짜느냐에 달려 있습니다.

| 나중에 JPA로 매끄럽게 교체됨 | 나중에 애먹음 |
|---|---|
| `Article save(Article a)` (id가 채워진 객체 반환) | `void add(Article a)` |
| `Optional<Article> findById(Long id)` | `Article get(Long id)` (null 반환) |
| `List<Article> findByUserId(Long userId)` | `List<Article> getMyList()` |
| `void deleteById(Long id)` | `void remove(Article a)` |

- **`Vocabulary`는 `Long articleId`로 참조 (Article이 `List<Vocabulary>`를 들고 있는 방식 X)** — 나중에 JPA의 `@ManyToOne`으로 자연스럽게 이어지고, cascade/orphanRemoval/지연로딩 관련 초보자용 함정을 피할 수 있음
- 테스트 전용 메서드(`clearStore()` 등)는 인터페이스가 아니라 메모리 구현 클래스에만 두기 — JPA엔 대응이 없음
- ID는 `Long`으로 통일 (`Map<Long, Article>`인데 `int`로 조회하면 `Integer`≠`Long`이라 조용히 `null` 반환됨), `store.size()+1`로 채번하지 말 것 (삭제 후 중복 발생) → `AtomicLong` 사용

## 6. 시작 전 결정할 것 — Spring Boot 버전

`build.gradle`이 **Spring Boot 4.1.1**로 고정되어 있고 (`spring-boot-starter-webmvc`라는 이름 자체가 Boot 4 전용 네이밍), 세상의 거의 모든 Spring 입문 튜토리얼/강의는 아직 2.x/3.x 기준입니다. 코드 자체는 정상 동작하지만, 튜토리얼을 따라 하다 "이게 왜 강의랑 다르지?"에 부딪힐 일이 잦을 것입니다.

- **3.5.x로 다운그레이드 권장**: 배우려는 개념(DI, MVC, Thymeleaf, 세션, JPA)은 3.5와 4.1에서 동일하며, 튜토리얼과 1:1로 맞아떨어져 학습 마찰이 훨씬 적습니다.
- 4.1.1을 유지하고 싶다면: Stage 0에서 JUnit5/AssertJ가 테스트 스타터 분리 이후에도 잘 잡히는지부터 확인하고 시작할 것.

## 7. 초보자가 특히 자주 겪는 함정 (미리 알아두면 좋은 것)

1. **BCrypt는 `equals()`로 비교 불가** — 매번 다른 해시가 나오므로 반드시 `encoder.matches(원문, 저장된해시)` 사용
2. **BCrypt 하나 쓰려고 `spring-boot-starter-security` 전체를 넣지 말 것** — 전체 필터체인이 자동 설정되어 모든 엔드포인트가 잠기고 폼 POST가 CSRF에 막힘. `org.springframework.security:spring-security-crypto`만 추가하면 `BCryptPasswordEncoder`만 가져올 수 있음
3. **메모리 Repository는 객체 참조를 그대로 돌려줌** → `findById`로 꺼낸 객체를 수정하면 `save()` 안 불러도 이미 저장된 것처럼 동작함. Stage 8에서 트랜잭션 밖이면 조용히 안 먹혀서 "JPA가 고장났다"로 오해하기 쉬움 → 수정 후엔 항상 명시적으로 `save()` 호출하는 규칙을 스스로 정해둘 것
4. **인터셉터에서 로그인/회원가입 경로를 `excludePathPatterns`에서 빼먹으면** 로그인 페이지 자체가 인터셉트되어 무한 리다이렉트 발생
5. **체크 안 한 체크박스는 아예 전송되지 않음** — 팝업에서 하나도 안 체크하면 파라미터 자체가 없으므로 `@RequestParam(required = false)`로 받고 null 처리 필요
6. **Thymeleaf 3.1+에서 `#session`/`#httpSession` 등이 제거됨** — 오래된 예제 코드가 파싱 에러남. 세션 값은 인터셉터나 `@ModelAttribute` 메서드로 `Model`에 담아 넘길 것
7. **랜덤 3개 뽑기**: 단어가 3개 미만이면 `subList(0,3)`이 예외 발생 → `Math.min(3, size)` 사용, `subList`는 원본 뷰이므로 `new ArrayList<>(...)`로 복사

## 8. 범위 조정 제안

- **이대로 진행해도 좋은 범위** — 각 개념이 한 단계씩 깔끔하게 나뉘고, 하나만 유독 어려운 게 없음
- **추가로 빼는 게 좋은 것**: Spring Security(이번엔 제외, 다음 프로젝트로), 고급 유효성 검사(커스텀 Validator 등), 페이지네이션/검색/태그 — 요구사항에도 없고 넣을 필요 없음
- **단순화 권장**: 계층마다 별도 DTO를 만들지 말고, 처음엔 도메인 객체를 뷰에 그대로 전달 → 회원가입의 `passwordConfirm`처럼 정말 다른 경우에만 폼 DTO 분리
- **의외로 넣을 가치가 있는 것**: Stage 1부터 테스트 작성. Repository 계약을 명확히 하게 되고, Stage 8에서 JPA 구현체에 동일 테스트를 그대로 돌려볼 수 있어 이후 단계가 훨씬 수월해짐

## 9. 결론

**판단: GO (학습 프로젝트로 진행 권장)**

- **기술적 실현 가능성**: 높음 — 모든 요구사항이 Spring Boot의 표준 기능으로 커버되고, 그린필드라 기존 코드와 충돌할 게 없음
- **전체 난이도**: 중간 — 개별 개념 하나하나는 어렵지 않지만, 인증+세션+템플릿+연관 엔티티 2개+데이터 계층 교체까지 폭이 넓어서 합치면 중간 수준
- **가장 큰 리스크**: 기술적 리스크가 아니라 "생태계" 리스크 — Boot 4.1.1과 3.x 기준 튜토리얼 자료 간의 불일치 (§6). Stage 0에서 버전을 먼저 결정하면 이후는 순조로움

## 10. 다음 단계

1. §6의 Spring Boot 버전 결정 (3.5.x 다운그레이드 권장)
2. `/rpi:plan "article-vocab-tracker"` 실행 → 위 Stage 0~9를 단계별 실행 계획(PLAN.md)으로 구체화
