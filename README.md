# 🚀 FastPick Backend

> **100만 건 데이터 조회와 선착순 쿠폰 발급 환경에서 발생하는  
> 검색 병목과 동시성 문제를 재현하고, 처리 구조를 개선해 결과를 검증한 프로젝트**

FastPick은 한정 수량 쿠폰을 선착순으로 발급하는 시스템입니다.

단순 기능 구현보다 부하 테스트와 실행 계획 분석을 통해 병목을 확인하고,  
데이터 탐색 방식과 발급 처리 흐름을 변경한 뒤 지표로 검증하는 데 집중했습니다.

---

## 📈 핵심 성과

| 구분              |                개선 전 |           개선 후 |
| ----------------- | ---------------------: | ----------------: |
| 검색 API p99      |              60초 초과 |         **154ms** |
| 검색 Error Rate   |                 약 75% |            **0%** |
| 검색 처리량       |             약 7.5 TPS |      **300+ TPS** |
| 동시 1,000건 발급 | 락 경합 및 커넥션 대기 | **초과 발급 0건** |

---

## 🔍 대용량 데이터 조회 성능 개선

### 문제

- 100만 건 데이터 환경에서 `LIKE '%keyword%'` 검색 사용
- 선행 와일드카드로 B-Tree 인덱스를 활용하지 못함
- Full Table Scan과 I/O 병목 발생

### 개선

- 검색용 Keyword 테이블 분리
- 전방 일치 검색(`keyword%`)으로 탐색 구조 변경
- Projection으로 조회 범위 축소
- Redis Cache로 반복 조회 부하 감소

### 결과

- p99 **60초 초과 → 154ms**
- Error Rate **약 75% → 0%**
- **300+ TPS** 달성

📄 [대용량 데이터 조회 성능 개선 상세](docs/04-search.md)

---

## ⚡ 선착순 쿠폰 발급 동시성 제어

### 문제

- DB 비관적 락과 Redis 분산 락 기반 처리
- 동시 요청 증가 시 락 경합과 커넥션 대기 발생
- 경쟁 요청이 DB 트랜잭션 내부로 집중

### 개선

- Redis Atomic Counter로 발급 가능 여부를 사전 판단
- 발급 성공 요청만 Redis Queue에 적재
- Scheduler가 Queue를 조회하여 DB에 발급 이력을 저장
- 사용자 요청 처리와 DB 영속화 작업 분리

### 처리 흐름

```text
Client
  ↓
Spring Boot API
  ↓
Redis Atomic Counter
  ├─ 발급 불가 → 즉시 실패 응답
  └─ 발급 가능
        ↓
    Redis Queue 적재
        ↓
      성공 응답

Scheduler → Redis Queue 조회 → PostgreSQL 저장
```

### 결과

- 동시 **1,000 VU** 환경에서 초과 발급 **0건**
- 요청 1,000건 중 **999건 정상 처리**
- 1건은 네트워크 I/O 오류이며 비즈니스 로직 오류는 발생하지 않음

📄 [선착순 쿠폰 발급 동시성 제어 상세](docs/05-concurrency.md)

---

## 🏛 아키텍처

도메인별 계층을 다음과 같이 분리했습니다.

```text
[domain].ui
[domain].application
[domain].domain
[domain].infra
```

- UI와 기술 구현체가 도메인 및 애플리케이션 계층을 직접 지배하지 않도록 구성
- 공통 API 응답 및 예외 처리 규격 적용
- Testcontainers 기반 통합 테스트
- 예외 발생 시 Discord Webhook 알림

📄 [아키텍처 및 규약](docs/02-architecture.md)

---

## 🛠 기술 스택

| 영역        | 기술                                        |
| ----------- | ------------------------------------------- |
| Backend     | Java 17 · Spring Boot 3.5.9                 |
| Data        | PostgreSQL 17 · Redis                       |
| ORM / Query | Spring Data JPA · Querydsl 5.1.0            |
| Concurrency | Redis Atomic Counter · Redisson 3.24.3      |
| Test        | JUnit 5 · Testcontainers                    |
| Monitoring  | Spring Boot Actuator · Prometheus · Grafana |
| Load Test   | K6                                          |

---

## 🚀 로컬 실행

```bash
cp .env.example .env
docker-compose up -d
./gradlew bootRun
```

📄 [로컬 실행 가이드](docs/01-run-local.md)

---

## 📚 상세 문서

- [로컬 실행 가이드](docs/01-run-local.md)
- [아키텍처 및 규약](docs/02-architecture.md)
- [테스트 및 REST Docs](docs/03-testing.md)
- [대용량 데이터 조회 성능 개선](docs/04-search.md)
- [선착순 쿠폰 발급 동시성 제어](docs/05-concurrency.md)

---

## 📘 추가 기록

- [서버 인프라 구성도](https://www.notion.so/2f555588581980378187cd547219c018?source=copy_link)
- [대규모 데이터 조회 성능 튜닝](https://www.notion.so/2fc55588581980c3bebee8aa571b661e?source=copy_link)
- [실시간 쿠폰 발급 동시성 제어](https://www.notion.so/2fc55588581980d4b637f5ea13085351?source=copy_link)

---

## 🔗 관련 리포지토리

[FastPick Infra](https://github.com/maximum-zero/fastpick-infra)
