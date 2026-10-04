# 실행 방법

## 바로 실행 방법

```bash
# 백엔드 (H2 인메모리 + 테스트 데이터 자동 삽입)
cd Screening/backend/screening
./gradlew bootRun

# 프론트엔드 (새 터미널)
cd Screening/front/screening_front
npm install && npm run dev
# → http://localhost:5174
```

**테스트 계정**: `test01 / 1234` (신용점수 820, 연소득 6500만, 재직 48개월로 자동 세팅되어 있어서 바로 심사 흐름 테스트 가능)

Oracle DB가 필요할 때는 `application-local.yml`을 Oracle 설정으로 교체하면 됩니다.
