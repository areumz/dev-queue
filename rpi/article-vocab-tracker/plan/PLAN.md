# PLAN.md — Article & Vocabulary Tracker (dev-queue) 구현 로드맵

> 이 PLAN은 "최소 공수로 완성"이 아니라 **개념을 순서대로 익히는 것**을 최우선 목표로 짜여 있다. 각 Phase는 눈에 보이는 결과물로 끝나며, 다음 Phase는 이전 결과물 위에 쌓인다. 실제 코드는 직접 작성하며 개념을 익힌다 — 이 문서는 각 단계에서 "무엇을, 왜, 어떤 순서로" 할지에 대한 안내다.

## 진행 방식

- 각 Phase 끝의 **완료 기준(DoD)**을 스스로 통과했는지 확인한 뒤 다음 Phase로 넘어갈 것
- 복잡도 표기: Simple / Medium / Complex (개별 태스크 기준)
- 막히면 eng.md §11 "알려진 함정" 표를 먼저 확인

---

## Phase 0 — 환경 결정 & 스캐폴딩

**목표**: 프로젝트 뼈대를 세우고, 목업 데이터로 첫 화면을 띄운다.
**의존성**: 없음 (시작점)

### 태스크
1. **[Simple] Spring Boot 버전 결정** — 3.5.x로 다운그레이드할지, 4.1.1을 유지할지 결정 (eng.md §1.1 참고, 권장: 3.5.x). *다른 모든 작업의 선행 조건.*
2. **[Simple] 패키지 구조 생성** — `domain`, `repository`, `repository.memory`, `service`, `controller`, `config` 패키지 생성 (eng.md §2)
3. **[Simple] 리스트 화면 하드코딩 렌더링** — `list.png` 목업을 참고해 `@Controller`에서 가짜 데이터(3줄)를 `Model`에 담아 Thymeleaf로 렌더링. 아직 도메인 클래스/Repository 없음.
4. **[Simple] 기존 `HelloController` 정리 여부 결정** — 당장 지워도 되고, Phase 1 이후 정리해도 무방

### 개념
`@Controller`, `Model`, 뷰 리졸빙, `th:each`/`th:text`, 정적 리소스 서빙

### 완료 기준 (DoD)
- [x] Spring Boot 버전 결정 완료, `build.gradle` 반영
- [x] `list.png`와 유사한 형태로 가짜 데이터 3줄이 화면에 렌더링됨
- [x] 애플리케이션이 별도 에러 없이 기동됨

---

## Phase 1 — 도메인 모델 + 메모리 Repository (웹 계층 없이)

**목표**: "자바 객체 모델링"과 "Spring의 마법(DI)"을 분리해서 각각 이해한다.
**의존성**: Phase 0

### 태스크
1. **[Simple] `Category` enum 정의** — 개발문서/AI/채용정보/기타 (eng.md §4)
2. **[Medium] `Article` 도메인 클래스** — eng.md §4 필드 그대로, `userId`는 이 시점엔 없어도 됨(Stage 4b에서 추가 예정이므로 미리 넣어둬도 무방)
3. **[Medium] `ArticleRepository` 인터페이스 설계** — eng.md §5 규칙 준수 (`save`, `findById`→`Optional`, `findAll`, `deleteById`)
4. **[Medium] `MemoryArticleRepository` 구현** — `ConcurrentHashMap<Long, Article>` + `AtomicLong`으로 id 채번
5. **[Medium] 순수 JUnit5 테스트 작성** — Spring 컨테이너 없이 save/findById/findAll/delete 검증
6. **[Simple] `@Repository`/`@Service` 애너테이션 부착 후 `@SpringBootTest`로 DI 조립 확인**

### 개념
Enum 필드, 인터페이스, `Optional<T>`, `ConcurrentHashMap`/`AtomicLong`, JUnit5, DI, 싱글톤 스코프, 생성자 주입

### 완료 기준 (DoD)
- [x] save/findById/findAll/deleteById 순수 JUnit 테스트 전부 통과
- [x] `@SpringBootTest`에서 Service가 정상 주입됨
- [x] eng.md §5 규칙(특히 id 채번 방식, `Optional` 반환)을 지켰는지 셀프 체크

---

## Phase 2 — 아티클 CRUD (웹 계층 연결)

**목표**: Phase 0의 하드코딩 데이터를 실제 Repository로 교체하고, 전체 CRUD 흐름을 완성한다.
**의존성**: Phase 1

### 태스크
1. **[Medium] 리스트 화면을 실제 Repository 조회로 교체**
2. **[Medium] 새 링크 등록 폼** (`form.png` 참고) — GET 폼 → POST 저장 → redirect `/articles` (US-5)
3. **[Medium] 상세 조회 화면** (`note.png` 참고, 단어 섹션은 Phase 5에서 채움)
4. **[Simple] 메모 수정** (US-8)
5. **[Simple] 읽음 여부 토글** (US-9) — 자동/수동 여부는 이 단계에서 직접 결정 (pm.md 미결 사항)

### 개념
`@PostMapping`, `@ModelAttribute` 폼 바인딩, `th:object`/`th:field`, `@PathVariable`, POST-Redirect-GET 패턴, 폼 DTO vs 도메인 객체 구분

### 완료 기준 (DoD)
- [x] 등록 → 리스트 반영 → 상세 조회 → 메모 수정 전체 흐름이 서버 재시작 전까지 유지됨
- [x] 새로고침 시 폼 중복 제출 없음 (Redirect 패턴 확인)

---

## Phase 3 — 카테고리 필터

**목표**: 리스트 화면에 카테고리 탭 필터를 추가한다.
**의존성**: Phase 2

### 태스크
1. **[Simple] `@RequestParam(required=false) Category category`로 필터 파라미터 수신**
2. **[Simple] "전체" 탭은 파라미터 생략** — 빈 문자열 전달 금지 (eng.md §8, 400 에러 원인)
3. **[Simple] 활성 탭 강조** — `th:classappend`
4. **[Simple] 필터 로직은 Service 레이어에 위치** (뷰나 컨트롤러가 아니라)

### 개념
Enum 타입 `@RequestParam`, 조건부 CSS 클래스, 서비스 레이어 책임 분리

### 완료 기준 (DoD)
- [x] 4개 탭(전체/개발문서/AI/채용정보/기타 — 총 5개) 전환 시 정확한 필터링
- [x] "전체" 탭에서 400 에러 없음

---

## Phase 4 — 인증 (회원가입/로그인/세션/소유권)

**목표**: 세션 기반 인증을 직접 구현하고, 아티클에 소유자 개념을 붙인다. 이 프로젝트에서 가장 개념 밀도가 높은 Phase.
**의존성**: Phase 2 (Phase 3과는 독립적, 순서 바꿔도 무방)

### 태스크 — 4a. 인증 자체
1. **[Simple] `spring-security-crypto` 의존성만 추가** (Security 스타터 전체 금지 — eng.md §11 #2)
2. **[Simple] `PasswordEncoder` `@Bean` 등록** (`BCryptPasswordEncoder`)
3. **[Medium] `User` 도메인 + `UserRepository`(memory) + 순수 테스트**
4. **[Medium] 회원가입** — 아이디 중복 검사, `encoder.encode()`로 저장 (US-1)
5. **[Medium] 로그인** — `encoder.matches()`로 검증 (⚠️ `equals()` 금지, eng.md §11 #1), 성공 시 세션 저장 (US-2)
6. **[Simple] 로그아웃** — `session.invalidate()` (US-3)
7. **[Medium] `LoginCheckInterceptor` + `WebMvcConfigurer` 등록** — `/login`, `/signup`, 정적 리소스 반드시 `excludePathPatterns`에 포함 (eng.md §11 #4, 누락 시 무한 리다이렉트)
8. **[Medium] `@Valid` + `BindingResult`로 폼 검증**, 전역 에러/플래시 메시지 처리

### 태스크 — 4b. 소유권 연결 (인증 자체가 끝난 뒤 진행 권장)
9. **[Medium] `Article`에 `userId` 필드 연결** (Phase 1에서 이미 넣었다면 채우기만)
10. **[Medium] 리스트/상세를 로그인 사용자 소유 것만 보이도록 리팩터링** (US-6)
11. **[Medium] 타 사용자 리소스 직접 URL 접근 시 404/403 처리** (ux.md 엣지케이스)

*아티클 CRUD를 먼저 만들고 나중에 소유권을 붙이는 순서를 권장한다 (더 빨리 눈에 보이는 결과가 나오고, 메모리 단계라 리팩터링 비용도 낮음) — 이미 Phase 순서 자체가 이를 반영함.*

### 개념
`HttpSession`, `PasswordEncoder` Bean 등록, BCrypt `encode`/`matches`, `HandlerInterceptor`+`WebMvcConfigurer`, `@Valid`+`BindingResult`, 전역 에러 처리, 플래시 메시지

### 완료 기준 (DoD)
- [x] 로그아웃 상태로 보호된 페이지 접근 시 로그인 화면으로 리다이렉트됨
- [x] 계정 두 개로 가입 후 각각 로그인 시 서로 다른 리스트가 보임 (US-6 검증)
- [x] 로그인/회원가입 페이지 자체는 인터셉트되지 않음 (무한 리다이렉트 없음)
- [x] 비밀번호가 평문이 아닌 BCrypt 해시로 저장되어 있음을 직접 확인

---

## Phase 5 — 단어(Vocabulary) 관리

**목표**: Article과 1:N 관계인 Vocabulary를 구현하고, 상세 화면에서 관리한다.
**의존성**: Phase 2 (Phase 4와는 독립적으로 병행 가능)

### 태스크
1. **[Simple] `Vocabulary` 도메인 클래스** — `articleId`로 참조 (Article이 리스트를 들고 있지 않음, eng.md §5)
2. **[Medium] `VocabularyRepository` + `MemoryVocabularyRepository`** — `findByArticleId` 포함
3. **[Medium] 상세 화면에 단어 섹션 연결** (`note.png` 참고) — 단어 추가 (US-10)
4. **[Simple] 암기 여부 토글** (US-11)
5. **[Simple] 단어 삭제** (US-12)

### 개념
FK 필드로 1:N 표현, `findByArticleId` 같은 조회 메서드, 서비스 레이어에서 여러 Repository 조합

### 완료 기준 (DoD)
- [ ] 상세 화면에서 단어 추가/토글/삭제가 모두 정상 동작
- [ ] 서로 다른 아티클의 단어가 섞이지 않음

---

## Phase 6 — 단어 암기 팝업 (클라이맥스)

**목표**: 이 프로젝트의 핵심 기능. 여러 개념(Repository 조합, 세션 생명주기, 랜덤 알고리즘)을 동시에 요구하는 첫 지점 — 앞 Phase들보다 체감 난이도가 한 단계 올라가는 것이 정상.
**의존성**: Phase 4 (세션 인증), Phase 5 (Vocabulary)

### 태스크
1. **[Medium] `User`에 날짜 기반 팝업 노출 상태 추가** (`lastPopupDate: LocalDate`)
   - (변경) 원래는 세션 기반 1회성 플래그(`popupShown`)로 설계했으나, 로그인 세션을 유지하는 경우도 있으므로 하루가 지나도 팝업이 다시 뜰 수 있도록 세션이 아닌 User 도메인의 날짜 필드로 변경
   - 리스트 화면 진입 시 오늘 날짜와 `lastPopupDate`를 비교하여, 다르면 팝업 노출 + 날짜 갱신
2. **[Complex] 팝업 대상 단어 수집 로직** — 로그인 사용자 소유 Article들의 id 목록 조회 → 해당 Article들의 안 외운(`memorized=false`) Vocabulary 전체 수집 (서비스 레이어에서 ArticleRepository + VocabularyRepository 조합)
3. **[Medium] 랜덤 추출** — `Collections.shuffle()` + `Math.min(3, size)`로 `subList` 후 `new ArrayList<>()`로 복사 (eng.md §11 #7, size<3 예외 방지)
4. **[Medium] 팝업 프래그먼트 렌더링** (`words-popup.png` 참고) — 안 외운 단어 0개면 팝업 미노출 (US-13 AC)
5. **[Medium] 체크된 단어만 암기완료 처리** — `@RequestParam(required=false) List<Long> ids`로 수신, null이면 빈 리스트 취급 (eng.md §11 #5)
6. **[Simple] 처리 후 세션 플래그 제거** — 새로고침 시 재노출 안 되도록 (US-15)

### 개념
Thymeleaf 프래그먼트, `Collections.shuffle`+안전한 `subList`, `@RequestParam(required=false) List<Long>`, 세션의 1회성 플래그, 빈 상태 처리

### 완료 기준 (DoD)
- [x] 안 외운 단어가 3개 이상일 때 정확히 3개만 랜덤 노출
- [x] 안 외운 단어가 1~2개일 때 있는 만큼만 노출, 0개일 때 팝업 미노출
- [x] 체크한 단어만 암기완료로 바뀌고 나머지는 유지
- [x] 같은 날짜 안에서는 재접속해도 팝업이 다시 뜨지 않음 (변경: 세션이 아닌 날짜 기준)
- [x] 로그인을 계속 유지한 상태로 날짜가 바뀌면, 다음 접속 시 팝업이 다시 노출됨 (추가된 기준)

---

## Phase 7 — 다듬기 (선택, 생략 가능)

**목표**: 에러 처리와 스타일을 정리한다. 학습 흐름을 막지 않도록 의도적으로 맨 뒤에 배치.
**의존성**: Phase 0~6 중 다듬고 싶은 범위까지

### 태스크
1. **[Simple] `@ControllerAdvice`로 전역 에러 페이지**
2. **[Simple] `messages.properties`로 메시지 국제화/중앙화** (선택)
3. **[Medium] 목업에 맞춘 CSS 스타일링**
4. **[Medium] `@WebMvcTest`/`MockMvc`로 컨트롤러 테스트 보강** (선택)

### 완료 기준 (DoD)
- [ ] 원하는 범위만큼 완료 (전부 선택 사항)

---

## Phase 8 — JPA + MySQL 전환 (명확한 경계, 별도 세션에서 진행 권장)

**목표**: 메모리 구현체를 JPA/MySQL 구현체로 교체한다. Phase 1의 인터페이스 설계가 옳았는지 검증하는 단계.
**의존성**: Phase 0~6 완료 (Phase 7은 선택)

### 태스크
1. **[Simple] `spring-boot-starter-data-jpa` + MySQL 드라이버 의존성 추가**
2. **[Medium] 도메인 클래스에 `@Entity`/`@Id`/`@GeneratedValue`/`@Enumerated(STRING)`/`@ManyToOne(LAZY)` 부착**
3. **[Medium] `SpringDataJpaXxxRepository extends JpaRepository<T, Long>` 작성 후 기존 인터페이스에 연결**
4. **[Medium] `@Profile("memory")`/`@Profile("jpa")`로 구현체 전환 가능하게 구성**
5. **[Simple] Phase 1에서 작성한 Repository 계약 테스트를 JPA 구현체에 재실행**

### 개념
`@Entity`/`@Id`/`@GeneratedValue`/`@Enumerated(STRING)`/`@ManyToOne(LAZY)`, `JpaRepository<T,Long>`, `@Transactional`, 영속성 컨텍스트/dirty checking

### 완료 기준 (DoD) — **이 프로젝트의 최종 성공 지표**
- [ ] **Controller/Service 코드를 한 줄도 바꾸지 않고** 구현체 교체만으로 애플리케이션이 동일하게 동작함
- [ ] Phase 1의 Repository 계약 테스트가 JPA 구현체에서도 통과
- [ ] eng.md §11 #3 (메모리 Repository의 참조 공유 문제)이 JPA 전환 후 트랜잭션 경계 이해로 이어졌는지 스스로 점검

---

## Phase 9 (범위 밖, 다음 프로젝트) — Spring Security

직접 만든 세션 인증을 Spring Security 필터 체인으로 교체하는 것은 개념 도약폭이 커서 이번 프로젝트 범위에 포함하지 않는다. 별도 학습 프로젝트로 진행 권장.

---

## 전체 요약

| Phase | 이름 | 복잡도 | 태스크 수 | 선행 조건 |
|---|---|---|---|---|
| 0 | 환경 결정 & 스캐폴딩 | Simple | 4 | 없음 |
| 1 | 도메인 + 메모리 Repository | Simple→Medium | 6 | Phase 0 |
| 2 | 아티클 CRUD | Medium | 5 | Phase 1 |
| 3 | 카테고리 필터 | Simple | 4 | Phase 2 |
| 4 | 인증 (+소유권) | Medium | 11 | Phase 2 |
| 5 | 단어 관리 | Medium | 5 | Phase 2 |
| 6 | 단어 암기 팝업 | Medium→Complex | 6 | Phase 4, 5 |
| 7 | 다듬기 (선택) | Simple→Medium | 4 | Phase 0~6 |
| 8 | JPA/MySQL 전환 | Medium→Complex | 5 | Phase 0~6 |

**병렬 가능 구간**: Phase 3(카테고리 필터)과 Phase 4(인증)는 서로 독립적 — 순서를 바꾸거나 번갈아 진행 가능. Phase 5(단어 관리)도 Phase 4와 독립적으로 먼저 진행 가능(단, Phase 6은 Phase 4+5 둘 다 필요).

## 다음 단계

1. Phase 0의 태스크 1 (Spring Boot 버전 결정)부터 시작
2. 각 Phase의 DoD를 스스로 체크하며 순서대로 진행 (직접 구현)
3. 막히는 지점이 있으면 eng.md §11 함정 표와 RESEARCH.md를 먼저 참고
4. Phase 6까지 마치면 핵심 기능 완성 — Phase 7/8은 선택/후속 진행
