# 🚀 Fastpick Backend

FastPick은 한정 수량 쿠폰을 선착순으로 발급하는 시스템입니다.  
대규모 요청 환경에서 발생하는 검색 병목과 동시성 문제를 재현하고, 구조를 변경하여 개선한 프로젝트입니다.

본 프로젝트는 단순 기능 구현이 아니라, 부하 테스트를 통해 병목을 수치로 확인하고 설계 변경 후 지표로 검증하는 과정에 집중했습니다.

---

## 📊 성능 개선 요약
- 100만 건 데이터 환경에서 검색 병목 발생 (p99: 60s 초과, Error Rate: 약 75%)
- 동시 1000명 발급 요청 환경에서 락 경합 및 커넥션 대기 발생
- 단순 튜닝이 아닌 구조 변경을 통해 개선

---

## 1. 대용량 데이터 조회 성능 개선

### 문제
- `LIKE '%keyword%'` 조건으로 Full Table Scan 발생
- 인덱스를 활용할 수 없는 탐색 구조로 인해 I/O 병목 발생

### 개선
- Keyword 전용 테이블 분리
- 전방 일치(`keyword%`) 기반 탐색 구조로 변경
- Projection 적용 및 Redis 캐시 도입

### 결과
- p99 60초 → 154ms
- Error Rate 75% → 0%
- TPS 300+ (VU 150 기준)

> 📄 상세: [대용량 데이터 조회 성능 개선](docs/04-search.md)

---

## 2. 선착순 쿠폰 발급 동시성 제어

### 초기 설계
- DB 비관적 락
- Redis 분산 락

### 구조 변경
- Redis Atomic Counter 기반 선착순 사전 판단
- 트랜잭션 범위 축소 및 큐 기반 비동기 처리 구조로 분리

### 결과
- 동시 1000 VU 환경 초과 발급 0건
- 요청 1000건 중 999건 정상 처리 (1건 네트워크 I/O 오류)

> 📄 상세: [선착순 쿠폰 발급 동시성 제어](docs/05-concurrency.md)

---

## 🏛 아키텍처

### 계층형 구조(DIP 기반)

- `[domain].ui`
- `[domain].application`
- `[domain].domain`
- `[domain].infra`

> 📄 상세: [아키텍처 및 규약](docs/02-architecture.md)

---

## 🛠 기술 스택

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

---

## 🚀 로컬 실행
- `.env.example`을 복사해 `.env`로 생성 후 실행

```bash
docker-compose up -d
./gradlew bootRun
```

> 📄 상세: [로컬 실행 가이드](docs/01-run-local.md)

---

## 📚 문서
> - [로컬 실행 가이드](docs/01-run-local.md) 
> - [아키텍처 및 규약](docs/02-architecture.md) 
> - [테스트 및 REST Docs](docs/03-testing.md) 
> - [대용량 데이터 조회 성능 개선 상세](docs/04-search.md) 
> - [선착순 쿠폰 발급 동시성 제어 상세](docs/05-concurrency.md) 

## 📘 참고 자료 (Notion)
> - [서버 인프라 구성도](https://www.notion.so/2f555588581980378187cd547219c018?source=copy_link)
> - [대규모 데이터 조회 성능 튜닝](https://www.notion.so/2fc55588581980c3bebee8aa571b661e?source=copy_link) 
> - [실시간 쿠폰 발급 동시성 제어](https://www.notion.so/2fc55588581980d4b637f5ea13085351?source=copy_link) 


## 🔗 관련 리포지토리
> - [FastPick Infra](https://github.com/maximum-zero/fastpick-infra)
> - [FastPick FE](https://github.com/maximum-zero/fastpick-fe)
