# 아키텍처 및 개발 규약

## 1. 계층 구조

FastPick Backend는 도메인별로 다음 계층을 분리하여 구성합니다.

```text
[domain]
├── ui
├── application
├── domain
└── infra
```

### `[domain].ui`

외부 요청을 애플리케이션에서 사용할 수 있는 형태로 변환합니다.

- API Controller
- Request DTO
- Response DTO
- 요청값 검증
- API 응답 변환

### `[domain].application`

유스케이스와 애플리케이션 흐름을 처리합니다.

- Service
- Processor
- 도메인 객체 조합
- 트랜잭션 단위 처리
- Repository Interface 호출

### `[domain].domain`

비즈니스 규칙과 핵심 도메인 모델을 관리합니다.

- Entity
- Domain Model
- Repository Interface
- 도메인 규칙

### `[domain].infra`

데이터베이스, Redis 및 스케줄러와 같은 기술 구현을 담당합니다.

- JpaRepository
- Repository 구현체
- Redis 연동
- Scheduler
- 외부 시스템 연동 구현체

---

## 2. 계층 분리 목적

계층을 분리한 목적은 다음과 같습니다.

- API 표현 방식과 비즈니스 로직 분리
- 핵심 도메인 규칙과 데이터 접근 기술 분리
- 기술 구현 변경에 따른 영향 범위 축소
- 도메인별 책임과 코드 위치 명확화
- 테스트 가능한 구조 유지

---

## 3. API 응답 규격

모든 API는 공통 응답 형식을 사용합니다.

### 성공 응답

- `ApiResponse<T>` 형식으로 반환
- 성공 코드: `S000`

### 오류 응답

- `ErrorResponse` 형식으로 반환
- 오류 유형은 `ErrorCode`로 관리

### 공통 예외 처리

- `GlobalExceptionHandler`에서 예외를 공통 처리
- 예외 유형에 따라 응답 코드와 메시지를 변환

---

## 4. 로깅 및 예외 알림

### 요청 추적

- 요청 단위의 로그 식별자를 사용
- 하나의 요청에서 발생한 로그를 동일한 식별자로 추적

### 예외 알림

- 처리되지 않은 예외 발생 시 Discord Webhook으로 알림 전송
- 로그와 알림을 통해 오류 발생 시점과 요청 흐름을 확인

> 상세한 예외 처리 및 알림 규격은 하단의 Notion 문서를 참고합니다.

---

## 5. 테스트 규약

### 데이터베이스 테스트

- Testcontainers를 사용하여 실제 PostgreSQL 컨테이너 기반으로 테스트
- 운영 데이터베이스와 유사한 환경에서 쿼리 및 영속화 동작 검증

### 테스트 구조

- `@Nested`를 활용해 시나리오 단위로 테스트 분리
- BDD 형태의 테스트 메서드 이름 사용

```text
[given]_[when]_[then]
```
