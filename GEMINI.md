# 🚀 FastPick 프로젝트 가이드

## 🛠 핵심 스택 & 설정
- Spring Boot 3.5.X / Java 17 / PostgreSQL / Redis
- **보안**: 민감 정보는 `.env`로 관리 (Git 제외)
- **JPA**: 모든 설정을 `JpaConfig`에서 통합 관리하며 Auditing 활성화

## 📂 패키지 구조 규약
도메인 중심 계층형 구조를 따르며, 응집도를 높이기 위해 도메인별로 관리한다.
- `[domain].ui`: API Controller, DTO (Web)
- `[domain].application`: 비즈니스 로직 (Service, Facade)
- `[domain].domain`: 핵심 비즈니스 (Entity, Repository 인터페이스)
- `[domain].infra`: 구현체 및 인프라 설정 (RepositoryImpl, 외부 API)

## 🏛️ 도메인 설계 원칙
- **YAGNI (You Ain't Gonna Need It)**: 추측에 의한 메서드 추가를 지양하며, 현재 필요한 비즈니스 로직에 집중한다.
- **주석 표준**: 핵심 비즈니스 메서드(`issue`, `calculateStatus` 등)에는 Javadoc 스타일(`@param`, `@return`)을 적용한다.
- **상속 구조**:
  - 생성/수정 이력 필요: `BaseEntity`
  - 생성 이력만 필요 (예: `IssuedCoupon`): `BaseCreateEntity`
- **연관 관계**: 지연 로딩(`FetchType.LAZY`)을 기본으로 한다.

## 💡 핵심 도메인 규칙
- **User**: 비밀번호는 반드시 인코딩된 상태(`rawPassword`)로 엔티티에 주입한다.
- **Coupon**: 상태 계산은 실시간으로 수행하며, 발급 전 `validate` 로직을 필수 통과한다.
- **IssuedCoupon**: 비즈니스 행위 중심 명칭을 사용하며(예: `UserCoupon` X), 유저 소유 개념을 명확히 한다.

## 🧪 테스트 코드 원칙
- **계층 구조**: `@Nested`를 사용하여 테스트 시나리오를 그룹화한다.
- **가독성**: 3단계 `@DisplayName` 전략 적용
  1. 최상위 클래스: "OO 도메인 단위 테스트"
  2. @Nested: "OO 행위/상태 테스트"
  3. @Test: "어떤 조건에서, 어떤 결과를 기대한다" (한글 서술)
- **네이밍**: `[테스트 대상]_[동작]_[결과/조건]` 형식을 따름 (BDD 스타일)
