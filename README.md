# 🚀 FastPick Backend

FastPick 프로젝트의 백엔드 API 서버입니다.

대규모 트래픽 환경에서 발생하는 성능 병목과 동시성 문제를 직접 재현하고, 
이를 구조적 설계 전환을 통해 해결하는 과정에 집중한 프로젝트입니다.

본 서버는 100만 건 데이터 조회 성능 최적화와 Redis 기반 선착순 쿠폰 발급 시스템의 
안정성을 핵심 문제로 다룹니다.

## 🏛️ Performance Optimization

본 프로젝트는 단순 기능 구현을 넘어, 성능 한계를 지표로 확인하고 
병목의 원인을 구조적으로 분리하는 과정에 초점을 맞췄습니다.

각 개선 항목은
**부하 테스트를 통해 병목을 재현하고, 
지표 분석을 기반으로 설계를 전환한 뒤 결과를 검증하는 흐름으로 진행되었습니다.**

> 📘 **관련 문서**
> - [대규모 데이터 조회 성능 튜닝](https://www.notion.so/2fc55588581980c3bebee8aa571b661e?source=copy_link)
> - [실시간 쿠폰 발급 동시성 제어](https://www.notion.so/2fc55588581980d4b637f5ea13085351?source=copy_link)

### 📈 1. 대용량 데이터 조회 최적화
- **이슈**
  - 100만 건 데이터 환경에서 키워드 검색 시 p(99) 60s 초과, Error Rate 약 75% 발생
- **분석**
  - `LIKE %...%` 조건으로 인한 Full Table Scan
  - Buffer Cache 오염(Buffer Pollution) 및 I/O 병목 확인
- **개선**
  - **Keyword 테이블 분리**를 통한 검색 조건 정규화
  - B-Tree Index Scan 유도
  - Docker 환경에서 `shm_size(1GB)` 증설
  - DB 병렬 워커 자원 경합 해소
- **결과**
  - **Error Rate 0.00%**
  - **TPS 300+ 달성 (VUs 150 기준)**
  - 200 VU 이상부터 CPU 사용률 상승으로 인한 자원 포화 징후 확인

### 📈 2. 선착순 발급 동시성 제어
- **이슈**
  - 동시 1,000명 쿠폰 발급 요청 시
    - DB Lock 경합
    - 응답 지연 및 실패 발생
- **개선**
  - Redis Atomic Counter 기반 선착순 사전 검증(Pre-filtering)
  - DB 인입 전 재고 검증을 수행하여 Lock 경합 제거
  - 발급 처리를 큐 기반 비동기 구조로 분리
- **결과**
  - 발급 수량이 충분한 경우 요청 전량 성공
  - 수량 초과 시 실패는 선착순 탈락으로만 발생
  - 초과 발급 0건
  - 동시 1000 VU 요청 중 **999건 발급 성공**
  - 1건 실패는 네트워크 I/O 오류로 확인되었으며, 비즈니스 로직 오류나 초과 발급은 발생하지 않음

## 🛠️ 기술 스택

### Backend & Database
- **Framework**: Spring Boot 3.5.9 / Java 17
- **Database**: PostgreSQL 17
- **Cache & Concurrency**: Redis
- **ORM/Query**: Spring Data JPA, Querydsl 5.1.0
- **Concurrency Control**: Redis Atomic Counter, Redisson 3.24.3

### Test & DevOps
- **Test**: JUnit 5, Testcontainers (PostgreSQL 17-alpine)
- **Monitoring**: Prometheus, Grafana, Spring Boot Actuator
- **Load Test**: K6

> 📘 **관련 문서**
> - [기술 스택 선택 과정](https://www.notion.so/History-2f6555885819800f974fcd06ffa85b58?source=copy_link)

## 🏛️ 핵심 설계 및 규약

### 1. 아키텍처 (DIP 준수 계층형 구조)
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

### 2. API 응답 및 예외 처리
- **성공 응답**
  - 모든 API는 `ApiResponse<T>` 형식으로 반환
  - 성공 코드: **`S000`**
- **에러 응답**
  - `ErrorResponse` + `ErrorCode` 규격 사용
- **예외 처리**
  - `GlobalExceptionHandler`를 통한 공통 처리

> 📘 **관련 문서**
> - [예외 처리 및 로깅 규격](https://www.notion.so/2fa555885819800b949bd760e090d394?source=copy_link)

### 3. 테스트 규약
- **DB 테스트**
  - Testcontainers 기반 실제 PostgreSQL 환경 사용
- **테스트 구조**
  - `@Nested`를 활용한 시나리오 단위 테스트
  - BDD 스타일 네이밍 ([given]_[when]_[then])

## 🚀 시작하기

### 1. 환경 요구사항
- `Java 17`
- `Docker`

```bash
docker-compose up -d
```

### 2. 환경 변수(Environment Variables) 설정

민감 정보는 환경 변수로 관리

#### **Database (PostgreSQL)**
- `DB_HOST`: 데이터베이스 호스트 주소 (예: `localhost`)
- `DB_PORT`: 데이터베이스 포트 (예: `5432`)
- `DB_NAME`: 연결할 데이터베이스명
- `DB_USERNAME`: 데이터베이스 사용자 계정
- `DB_PASSWORD`: 데이터베이스 비밀번호

#### **Cache & Concurrency (Redis)**
- `REDIS_HOST`: Redis 호스트 주소
- `REDIS_PORT`: Redis 포트 (기본: `6379`)
- `REDIS_PASSWORD`: Redis 비밀번호 (설정 시)

#### **Security & Notification**
- `SECRET_KEY`: JWT 서명 및 암호화에 사용되는 비밀 키
- `DISCORD_WEBHOOK_URL`: 모니터링 및 예외 알림용 Discord 웹훅 주소

> `application-local.yml`에서 `${VARIABLE_NAME}` 형태로 참조

### 3. 애플리케이션 실행

Profile: `local`

```bash
./gradlew bootRun
```

## 🧪 테스트 및 문서화

#### 테스트 실행

```bash
./gradlew test
```

#### API 문서 확인
Spring REST Docs 기반 문서 제공
- 주소: http://localhost:8080/docs/index.html 

```bash
./gradlew build
```

## 🔗 Related Repositories

- 🏗️ Infrastructure
  - [FastPick Infrastructure Repository](https://github.com/maximum-zero/fastpick-infra)

- 🎨 Frontend Application
  - [FastPick Frontend Repository](https://github.com/maximum-zero/fastpick-fe)
