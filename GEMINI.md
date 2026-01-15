# 🚀 FastPick 프로젝트 가이드

## 🛠 핵심 스택 & 설정
- Spring Boot 3.5.X / Java 17 / PostgreSQL 16 / Redis
- **보안**: 민감 정보는 `.env`로 관리 (Git 제외)

## 📂 패키지 구조 규약
도메인 주도 계층형 구조를 준수하며, 각 도메인 하위에 아래 패키지를 구성한다.
- `[domain].ui`: Controller, Web 관련 클래스
- `[domain].application`: Service, Facade 등 비즈니스 로직
- `[domain].domain`: Entity, Repository 인터페이스, 도메인 로직
- `[domain].infra`: Repository 구현체, 외부 API 연동, DB 설정 등

## 🏗 개발 원칙 (코드 생성 시 준수)
- **엔티티**: 외부 노출 금지 (항상 DTO 사용)
- **DB 스키마**: 테이블명은 `tb_` 접두사를 사용하며, 모든 필드는 가급적 `NOT NULL`과 적절한 `Size`를 명시한다.
- **명명 규칙**: Java는 `CamelCase`, DB(테이블/컬럼)는 `snake_case`를 기본으로 사용한다.

## 🧪 테스트 코드 원칙
- **`@DisplayName`**: **"어떤 조건에서, 어떤 결과를 기대한다"** 는 의미를 명확하게 전달하도록 한글로 서술합니다.
  - (예시) `@DisplayName("존재하지 않는 쿠폰 ID로 상세 정보를 조회하면 404 에러를 반환한다.")`
- **테스트 메서드명**: **`[테스트 대상]_[동작]_[결과/조건]`** 형식의 BDD(Behavior-Driven Development) 스타일을 적용하여, 메서드 이름만으로 테스트의 의도를 파악할 수 있도록 합니다.
  - (예시) `getCoupon_NotFound_WhenCouponDoesNotExist()`
