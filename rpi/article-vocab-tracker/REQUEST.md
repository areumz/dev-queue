# REQUEST: Article & Vocabulary Tracker (dev-queue)

## 프로젝트 성격

Java/Spring Boot를 처음 배우면서 진행하는 학습용 개인 프로젝트다.

- 복잡한 최적화나 고급 패턴보다는, 각 개념을 명확히 이해하고 직접 구현하며 배우는 것이 목적이다.
- 실제 구현은 본인이 직접 코드를 작성하며 학습할 예정이다 (AI가 코드를 대신 작성하지 않음).
- RESEARCH.md와 PLAN.md는 "실무 최적 설계"가 아니라 "개념을 단계적으로 익힐 수 있는 구조"로 정리한다.

## 기능 요구사항

### 인증
- 회원가입 / 로그인 (아이디, 비밀번호, 닉네임)
- 세션 기반 인증
- 비밀번호는 BCrypt로 암호화하여 저장

### Article 엔티티
- 제목
- URL
- 카테고리 (Enum: 개발문서 / AI / 채용정보 / 기타)
- 메모
- 읽음 여부
- 작성자 (User와 연관관계)

### Vocabulary 엔티티
- Article과 1:N 관계
- 단어 / 뜻 / 암기여부

### 단어 암기 팝업
- 로그인 시 암기여부=false인 단어 중 랜덤 3개를 팝업으로 노출
- 사용자가 체크한 단어만 암기완료(암기여부=true)로 처리

### 데이터 계층
- 1단계: Repository 인터페이스 + 메모리 구현체 (ArrayList/HashMap)
- 2단계(추후): MySQL/JPA 구현체로 교체

### 화면 (Thymeleaf)
- 로그인
- 회원가입
- 리스트 (카테고리 필터)
- 상세 (메모 + 단어 관리)
- 새 링크 등록 폼
- 단어 암기 팝업

## 참고 자료
- `mockup/login.png`, `mockup/list.png`, `mockup/note.png`, `mockup/form.png`, `mockup/words-popup.png`
- README.md: "A personal dev article & video queue with vocabulary tracking, built while learning Java/Spring Boot"

## 현재 코드베이스 상태
- Spring Boot 4.1.1 / Java 17 (Gradle)
- 의존성: spring-boot-starter-thymeleaf, spring-boot-starter-webmvc (테스트 스타터 포함)
- 실제 도메인 코드 없음 — `HelloController`(튜토리얼용)와 `DevqueueApplication`만 존재
- DB/JPA 의존성 아직 미추가 (요구사항대로 메모리 구현체 우선)
