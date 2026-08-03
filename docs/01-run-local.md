# 로컬 실행 가이드

## 1. 요구사항

로컬 실행을 위해 다음 환경이 필요합니다.

- Java 17
- Docker
- Docker Compose

---

## 2. 환경 변수 설정

프로젝트 루트의 `.env.example` 파일을 복사하여 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```

생성한 `.env` 파일에 로컬 실행에 필요한 환경 변수를 입력합니다.

> `.env` 파일은 저장소에 커밋하지 않습니다.

---

## 3. 인프라 실행

Docker Compose를 사용하여 PostgreSQL과 Redis를 실행합니다.

```bash
docker-compose up -d
```

컨테이너 실행 상태를 확인합니다.

```bash
docker-compose ps
```

---

## 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션이 정상적으로 실행되면 로컬 환경에서 API를 사용할 수 있습니다.

---

## 5. 테스트 실행

```bash
./gradlew test
```

테스트 실행 및 REST Docs 생성 방법은  
[테스트 및 REST Docs](03-testing.md)를 참고합니다.

---

## 6. 실행 종료

Docker Compose로 실행한 컨테이너를 종료합니다.

```bash
docker-compose down
```
