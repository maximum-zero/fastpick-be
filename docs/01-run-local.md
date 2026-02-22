# 로컬 실행 가이드

## 1. 요구사항
- Java 17
- Docker / Docker Compose

---

## 2. 환경 변수 설정
1) `.env.example`을 `.env`로 복사
2) 필요한 값을 채움

```bash
cp .env.example .env
```

---

## 3. 컨테이너 실행
```bash
docker-compose up -d
```

---

## 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

---

> 테스트 실행 및 REST Docs 생성 방법은 [테스트 및 REST Docs](docs/03-testing.md)를 참고
