# 0001. 테스트 DB로 Testcontainers(MySQL) 사용

- 상태: 확정 (2026-11-10)
- 사이클: v2-0a

## 맥락

`@SpringBootTest` 테스트가 로컬 MySQL에 붙어야 해서 CI에서 돌릴 수 없었다. 테스트용 DB가 필요했다.

## 선택지

- **H2**: 메모리 DB. 빠르고 Docker가 필요 없다. MySQL과 문법이 달라 테스트는 통과하고 운영에서 깨질 수 있다.
- **Testcontainers(MySQL 8)**: 테스트 때 진짜 MySQL 컨테이너를 띄운다. 운영과 같다. Docker가 필요하고 첫 기동이 몇 초 걸린다.

## 결정

Testcontainers(MySQL 8.0).

## 이유

- 운영과 같은 DB로 검증한다. v2-4 추천 기능의 `GROUP BY`, 셀프 조인처럼 MySQL 문법에 기대는 쿼리가 예정돼 있다.
- GitHub Actions 우분투 러너에 Docker가 기본으로 있다.
- `@ServiceConnection`으로 연결 설정을 자동으로 채워서 설정 코드가 거의 없다.

## 결과

- 로컬 테스트에 Docker가 필요하다.
- 첫 실행 때 `mysql:8.0` 이미지(약 600MB)를 받는다.
