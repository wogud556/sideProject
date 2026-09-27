# passage-quiz MCP 서버

지문(독해 텍스트)을 넣으면 객관식 문제를 만들어 주는 MCP 서버 샘플이다.

MCP 서버가 직접 LLM을 호출하지 않는 구조라 **API 키 없이 바로 돌아간다.** 규칙 기반으로 만들 수 있는 문제는 서버가 결정적으로(deterministic) 생성하고, 규칙으로는 어려운 추론·어휘 문제는 **Prompt**로 호스트 LLM(Claude)에게 넘긴다.

## 빠르게 실행

```bash
npm install
npm run build
npm run smoke     # 실제 MCP 클라이언트로 전체 기능 확인
```

`npm run smoke`는 서버를 stdio로 띄우고 도구 목록 조회 → 지문 분석 → 문제 생성 → 재현성 확인 → 채점 → 프롬프트 조회 → 오류 처리까지 한 바퀴 돈다.

## Claude Code / Claude Desktop에 붙이기

```bash
claude mcp add passage-quiz -- node /절대경로/passage_quiz_mcp/dist/index.js
```

또는 설정 파일에 직접:

```json
{
  "mcpServers": {
    "passage-quiz": {
      "command": "node",
      "args": ["/절대경로/passage_quiz_mcp/dist/index.js"]
    }
  }
}
```

## 제공하는 것

### Tools

| 이름 | 하는 일 |
|---|---|
| `analyze_passage` | 문단·문장 수, 핵심어, 대략적 난이도를 계산한다. 어떤 문제를 낼 수 있을지 먼저 가늠할 때 쓴다. |
| `generate_quiz` | 객관식 문제를 만든다. 네 가지 유형을 규칙 기반으로 생성한다. |
| `grade_quiz` | 생성한 문제지의 답안을 채점하고 문항별 해설을 붙인다. |

**문제 유형**

| `type` | 유형 | 정답 근거 |
|---|---|---|
| `blank` | 빈칸 추론 | 핵심어를 지우고 다른 핵심어를 오답으로 배치 |
| `order` | 문장 배열 | 연속한 세 문장을 섞음 — 정답이 구조적으로 보장됨 |
| `main_idea` | 중심 내용 | 핵심어 밀도가 가장 높은 문장을 주제문으로 추정 |
| `true_false` | 사실 확인 O/X | 원문 그대로면 O, 수치·서술어·핵심어를 뒤집으면 X |

`seed`를 고정하면 같은 지문에서 항상 같은 문제가 나온다. 반 전체에 같은 시험지를 돌릴 때 쓴다.

```
generate_quiz(passage: "...", count: 4, types: ["blank", "true_false"], seed: "2026-1학기-중간")
```

### Prompts

| 이름 | 하는 일 |
|---|---|
| `quiz_from_passage` | 규칙으로는 만들 수 없는 추론·어휘·서술형 문제를 호스트 LLM에게 출제시킨다. |
| `explain_answer` | 학생이 틀린 문항에 대해 지문 근거를 짚는 해설을 쓰게 한다. |

### Resources

- `passage://sample/ko-1` — 바로 시험해 볼 수 있는 샘플 지문(도시 열섬 현상)

## 구조

```
src/
├── korean.ts   문장 분리, 조사 처리, 핵심어 추출  ← 한국어 처리는 여기에만 있다
├── quiz.ts     문제 유형별 생성기 + 채점 + 시험지 렌더링
└── index.ts    MCP 서버 배선 (tools / prompts / resources)
```

`korean.ts`는 형태소 분석기 없이 동작한다. 조사 목록과 어미 패턴에 기대는 휴리스틱이라 완벽하지 않다. 정확도를 올리려면 이 파일만 `mecab-ko`나 `kiwi` 같은 형태소 분석기로 갈아 끼우면 되고, 나머지 코드는 손대지 않아도 된다.

## 알려진 한계

이 샘플이 규칙 기반으로 보장하는 것과 보장하지 않는 것을 구분해 둔다.

**믿어도 되는 것**
- `order` 문항의 정답 — 원문 순서를 섞은 것이므로 항상 옳다.
- `true_false`의 O 문항 — 지문 문장을 그대로 옮긴다.
- `seed` 재현성 — 같은 지문·같은 seed는 같은 문제지를 만든다.

**검토가 필요한 것**
- `main_idea`의 정답 — 핵심어 빈도로 주제문을 *추정*한다. 두괄식이 아닌 글에서는 틀릴 수 있다.
- `blank`의 오답 선택지 — 다른 핵심어를 그대로 쓰므로, 문맥상 명백히 안 맞아 쉬워질 때가 있다.
- 핵심어 추출 — 형태소 분석기가 없어 `열을` 같은 어절은 버리고, `회의`처럼 조사로 끝나 보이는 명사도 함께 버린다.

수업에 그대로 쓰려면 `generate_quiz`로 초안을 뽑고 `quiz_from_passage` 프롬프트로 다듬는 조합을 권한다.

## 라이선스

MIT
