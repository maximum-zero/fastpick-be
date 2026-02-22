# 테스트 및 REST Docs

## 1. 테스트 실행
- Testcontainers 기반으로 실제 PostgreSQL 컨테이너를 실행하여 통합 테스트를 수행

```bash
./gradlew test
```

---

## 2. REST Docs 생성
- 테스트 실행 과정에서 API 스니펫이 생성
- build 수행 시 REST Docs 문서가 구성

```bash
./gradlew clean build
```

## 3. 문서 확인
- 다음 경로에서 생성된 HTML 문서를 확인
    - build/docs/asciidoc/index.html