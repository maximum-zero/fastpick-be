# 🚀 FastPick 프로젝트 가이드

## 🛠 핵심 스택 & 설정
- **Framework**: Spring Boot 3.5.X / Java 17
- **Database**: PostgreSQL (Main), Redis (Cache/Lock)
- **Infrastructure**: Testcontainers (PostgreSQL 16-alpine) 기반 통합 테스트 환경
- **보안**: 민감 정보는 `.env`로 관리 (Git 제외)
- **JPA**: `JpaConfig`에서 모든 설정을 통합 관리하며 Auditing(`BaseEntity`) 활성화

## 📦 공통 응답 및 예외 규약
- **성공 응답**: 모든 API는 `ApiResponse<T>`를 반환하며, 성공 코드는 **`S000`**으로 통일한다.
- **에러 응답**: 예외 발생 시 `ErrorResponse`를 반환하며, `ErrorCode` 열거형에 정의된 규격을 따른다.
- **예외 처리**:
  - 비즈니스 예외는 `BusinessException`을 상속받아 구현한다.
  - `GlobalExceptionHandler`에서 모든 예외를 포착하여 공통 응답 규격으로 변환한다.

## 📂 패키지 및 저장소(Repository) 규약
도메인 중심 계층형 구조를 따르며, 의존성 역전(DIP)을 위해 아래와 같이 구성한다.

### 패키지 구조
- `[domain].ui`: API Controller, Request/Response DTO
- `[domain].application`: 비즈니스 로직 (Service, Facade)
- `[domain].domain`: 핵심 비즈니스 로직 (**Entity**, **Repository 인터페이스**)
- `[domain].infra`: 기술 구현체 (**JpaRepository**, **RepositoryImpl**)

### 저장소 구조 상세
1. **Repository (Interface)**: `domain` 패키지에 위치. 순수 자바 인터페이스로 비즈니스 요구사항 정의.
2. **JpaRepository (Interface)**: `infra` 패키지에 위치. Spring Data JPA 전용 인터페이스.
3. **RepositoryImpl (Class)**: `infra` 패키지에 위치. 1번 인터페이스를 구현하며, 내부에서 2번을 주입받아 사용.

## 🏛️ 도메인 설계 원칙
- **YAGNI (You Ain't Gonna Need It)**: 현재 필요한 비즈니스 로직에 집중하며 추측에 의한 메서드 추가를 지양한다.
- **주석 표준**: 핵심 비즈니스 메서드에는 Javadoc 스타일(`@param`, `@return`)을 적용한다.
- **상속 구조**:
  - 생성/수정 이력 필요: `BaseEntity` 상속
  - 생성 이력만 필요: `BaseCreateEntity` 상속
- **연관 관계**: 성능 최적화를 위해 지연 로딩(`FetchType.LAZY`)을 기본으로 한다.

## 🧪 테스트 코드 원칙
### 가독성 및 네이밍
- **계층 구조**: `@Nested`를 사용하여 테스트 시나리오를 그룹화한다.
- **3단계 DisplayName 전략**:
  1. 최상위 클래스: "OO 도메인 단위 테스트"
  2. @Nested: "OO 행위/상태 테스트"
  3. @Test: "어떤 조건에서, 어떤 결과를 기대한다" (한글 서술)
- **네이밍**: `[테스트대상]_[동작]_[결과]` 형식을 따름 (BDD 스타일)

### 기술 규격
- **Mocking**: Spring Boot 3.4+ 규격인 **`@MockitoBean`** 및 `@MockitoSpyBean`을 사용한다.
- **Database**:
  - `application-test.yml`의 **JDBC URL Scheme**(`jdbc:tc:postgresql...`)을 활용하여 실제 PostgreSQL 컨테이너에서 검증한다.
  - `@DataJpaTest` 적용 시 `@AutoConfigureTestDatabase(replace = Replace.NONE)`를 설정하여 YAML 설정을 유지한다.
- **Validation**: API 실패 테스트 시 응답 JSON의 `code`와 `message`가 `ErrorCode`와 일치하는지 `jsonPath`로 반드시 검증한다.
