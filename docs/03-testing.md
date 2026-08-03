# 테스트 및 REST Docs

## 1. 테스트 환경

FastPick Backend는 Testcontainers를 사용하여  
실제 PostgreSQL 컨테이너 기반의 통합 테스트를 수행합니다.

이를 통해 인메모리 데이터베이스가 아닌 운영 환경과 유사한 데이터베이스에서 다음 항목을 검증합니다.

- Entity 영속화
- Repository 쿼리
- 데이터베이스 제약조건
- PostgreSQL 기반 동작

---

## 2. 테스트 실행

프로젝트 루트에서 다음 명령을 실행합니다.

```bash
./gradlew test
```

테스트 결과는 다음 경로에서 확인할 수 있습니다.

```text
build/reports/tests/test/index.html
```

---

## 3. REST Docs 생성

API 문서 스니펫은 테스트 실행 과정에서 생성됩니다.

전체 빌드를 수행하면 테스트와 함께 REST Docs 문서가 구성됩니다.

```bash
./gradlew clean build
```

---

## 4. 생성 결과 확인

생성된 REST Docs HTML 문서는 다음 경로에서 확인합니다.

```text
build/docs/asciidoc/index.html
```

브라우저에서 해당 HTML 파일을 열어 API 문서를 확인할 수 있습니다.

---

## 5. 테스트 작성 규약

- 시나리오별 테스트는 `@Nested`를 사용하여 구분
- 테스트 이름은 BDD 형태로 작성

```text
[given]_[when]_[then]
```

- 데이터베이스 연동 테스트는 Testcontainers 기반 PostgreSQL을 사용
- 테스트가 서로 영향을 주지 않도록 독립적인 데이터 조건 구성
