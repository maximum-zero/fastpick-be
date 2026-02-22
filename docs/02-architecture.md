# 아키텍처 및 규약

## 1. 계층 구조 (DIP 기반)

- `[domain].ui`
    - API Controller
    - Request/Response DTO
- `[domain].application`
    - 비즈니스 로직
    - Service, Processor
- `[domain].domain`
    - 핵심 도메인 모델
    - Entity, Repository Interface
- `[domain].infra`
    - 기술 구현체
    - JpaRepository, RepositoryImpl, 외부 연동 (Scheduler)

---

## 2. API 응답 규격
- **성공 응답**
    - 모든 API는 `ApiResponse<T>` 형식으로 반환
    - 성공 코드: **`S000`**
- **에러 응답**
    - `ErrorResponse` + `ErrorCode` 규격 사용
- **예외 처리**
    - `GlobalExceptionHandler`를 통한 공통 처리

---

## 3. 로깅 및 예외 알림
- 예외 발생 시 Discord Webhook을 통한 알림 전송
- 요청 단위 추적을 위한 로그 식별자 포함

---

## 4. 테스트 규약
- **DB 테스트**
    - Testcontainers 기반 실제 PostgreSQL 환경 사용
- **테스트 구조**
    - `@Nested`를 활용한 시나리오 단위 테스트
    - BDD 스타일 네이밍 ([given]_[when]_[then])

---

## 📘 참고 자료 (Notion)
> - [예외 처리 및 로깅 규격](https://www.notion.so/2fa555885819800b949bd760e090d394?source=copy_link)
