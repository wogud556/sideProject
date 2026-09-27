import {
  extractKeywords,
  splitSentences,
  stripJosa,
  toEojeol,
  replaceNoun,
  withJosa,
  type Keyword,
} from './korean.js';

export const QUESTION_TYPES = ['blank', 'order', 'main_idea', 'true_false'] as const;
export type QuestionType = (typeof QUESTION_TYPES)[number];

const TYPE_LABEL: Record<QuestionType, string> = {
  blank: '빈칸 추론',
  order: '문장 배열',
  main_idea: '중심 내용',
  true_false: '사실 확인',
};

export interface Question {
  no: number;
  type: QuestionType;
  typeLabel: string;
  stem: string;
  body?: string;
  choices: string[];
  answerIndex: number;
  explanation: string;
}

export interface Quiz {
  quizId: string;
  createdAt: string;
  questionCount: number;
  questions: Question[];
}

/* ------------------------------------------------------------------ */
/* 같은 지문 + 같은 시드면 같은 문제가 나오도록 고정 난수를 쓴다.        */
/* ------------------------------------------------------------------ */

function hashString(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

function makeRandom(seed: number): () => number {
  let a = seed >>> 0;
  const next = () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  // 비슷한 시드끼리 초반 난수가 몰리는 것을 막는다. 워밍업이 없으면 O/X 문항이 한쪽으로 쏠린다.
  for (let i = 0; i < 16; i++) next();
  return next;
}

function shuffle<T>(items: T[], rand: () => number): T[] {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

/** 정답을 섞은 뒤 새 위치를 돌려준다. */
function shuffleChoices(
  choices: string[],
  answer: string,
  rand: () => number,
): { choices: string[]; answerIndex: number } {
  const shuffled = shuffle(choices, rand);
  return { choices: shuffled, answerIndex: shuffled.indexOf(answer) };
}

/* ------------------------------------------------------------------ */
/* 문제 유형별 생성기                                                   */
/* ------------------------------------------------------------------ */

/** 핵심어를 지우고 네 개의 선택지 중 고르게 한다. */
function makeBlank(
  sentences: string[],
  keywords: Keyword[],
  rand: () => number,
): Question | null {
  for (const kw of keywords) {
    const target = sentences.find((s) =>
      toEojeol(s).some((e) => stripJosa(e) === kw.word),
    );
    if (!target) continue;

    const distractors = keywords
      .filter((k) => k.word !== kw.word)
      .slice(0, 6)
      .map((k) => k.word);
    if (distractors.length < 3) return null;

    const blanked = target.replace(kw.word, '( ㉠ )');
    const picked = shuffle(distractors, rand).slice(0, 3);
    const { choices, answerIndex } = shuffleChoices(
      [kw.word, ...picked],
      kw.word,
      rand,
    );

    return {
      no: 0,
      type: 'blank',
      typeLabel: TYPE_LABEL.blank,
      stem: '다음 문장의 빈칸 ㉠에 들어갈 말로 가장 적절한 것은?',
      body: blanked,
      choices,
      answerIndex,
      explanation: `원문은 "${target}" 이다. ㉠에는 지문 전체에서 ${kw.count}회 반복되는 핵심어 '${kw.word}'가 들어간다.`,
    };
  }
  return null;
}

/** 연속한 세 문장을 섞어 원래 순서를 찾게 한다. 정답이 구조적으로 보장된다. */
function makeOrder(sentences: string[], rand: () => number): Question | null {
  if (sentences.length < 3) return null;

  const start = Math.floor(rand() * (sentences.length - 2));
  const window = sentences.slice(start, start + 3);
  const marks = ['(가)', '(나)', '(다)'];

  // 원문 순서 → 라벨 순서를 섞는다.
  const order = shuffle([0, 1, 2], rand);
  const labelled = order.map((originalIdx, i) => ({
    mark: marks[i],
    text: window[originalIdx],
    originalIdx,
  }));

  const correct = [...labelled]
    .sort((a, b) => a.originalIdx - b.originalIdx)
    .map((x) => x.mark)
    .join(' → ');

  const allOrders = [
    '(가) → (나) → (다)',
    '(가) → (다) → (나)',
    '(나) → (가) → (다)',
    '(나) → (다) → (가)',
    '(다) → (가) → (나)',
    '(다) → (나) → (가)',
  ];
  const distractors = shuffle(
    allOrders.filter((o) => o !== correct),
    rand,
  ).slice(0, 3);
  const { choices, answerIndex } = shuffleChoices(
    [correct, ...distractors],
    correct,
    rand,
  );

  return {
    no: 0,
    type: 'order',
    typeLabel: TYPE_LABEL.order,
    stem: '(가)~(다)를 글의 흐름에 맞게 배열한 것으로 가장 적절한 것은?',
    body: labelled.map((x) => `${x.mark} ${x.text}`).join('\n'),
    choices,
    answerIndex,
    explanation: `지문에서 세 문장은 ${correct} 순서로 이어진다.`,
  };
}

/** 핵심어 밀도가 가장 높은 문장을 주제문으로 본다. */
function makeMainIdea(
  sentences: string[],
  keywords: Keyword[],
  rand: () => number,
): Question | null {
  // 길이로 정답이 드러나지 않도록, 비슷한 길이의 문장끼리만 겨루게 한다.
  const pool = sentences.filter((s) => s.length >= 25);
  if (pool.length < 4) return null;

  const weight = new Map(keywords.map((k) => [k.word, k.count]));
  const ranked = pool
    .map((s) => {
      const words = toEojeol(s).map(stripJosa);
      const score = words.reduce((sum, w) => sum + (weight.get(w) ?? 0), 0);
      return { sentence: s, score: score / Math.max(words.length, 1) };
    })
    .sort((a, b) => b.score - a.score);

  const answer = clip(ranked[0].sentence);
  // 최하위만 모으면 오답이 티가 나므로 2등 아래에서 고르게 뽑는다.
  const rest = ranked.slice(1);
  const step = Math.max(1, Math.floor(rest.length / 3));
  const distractors = [rest[0], rest[step], rest[step * 2]]
    .filter(Boolean)
    .map((x) => clip(x.sentence))
    .filter((t, i, arr) => t !== answer && arr.indexOf(t) === i);
  if (distractors.length < 3) return null;

  const { choices, answerIndex } = shuffleChoices(
    [answer, ...distractors],
    answer,
    rand,
  );

  return {
    no: 0,
    type: 'main_idea',
    typeLabel: TYPE_LABEL.main_idea,
    stem: '이 글의 중심 내용으로 가장 적절한 것은?',
    choices,
    answerIndex,
    explanation:
      '핵심어가 가장 조밀하게 모인 문장을 주제문으로 골랐다. 빈도 기반 추정이므로 수업에 쓰기 전 검토가 필요하다.',
  };
}

const NEGATIONS: [RegExp, string][] = [
  [/증가/g, '감소'],
  [/감소/g, '증가'],
  [/높다/g, '낮다'],
  [/낮다/g, '높다'],
  [/상승/g, '하락'],
  [/커진다/g, '작아진다'],
  [/있다\.$/, '없다.'],
  [/이다\.$/, '이 아니다.'],
];

/** 원문을 그대로 쓰면 O, 수치나 서술어를 뒤집으면 X가 된다. */
function makeTrueFalse(
  sentences: string[],
  keywords: Keyword[],
  rand: () => number,
): Question | null {
  const usable = sentences.filter((s) => s.length >= 20);
  if (usable.length === 0) return null;

  const source = usable[Math.floor(rand() * usable.length)];
  const shouldBeTrue = rand() < 0.5;

  if (shouldBeTrue) {
    return {
      no: 0,
      type: 'true_false',
      typeLabel: TYPE_LABEL.true_false,
      stem: '다음 진술이 지문의 내용과 일치하면 O, 일치하지 않으면 X를 고르시오.',
      body: source,
      choices: ['O (일치한다)', 'X (일치하지 않는다)'],
      answerIndex: 0,
      explanation: '지문의 문장을 그대로 옮긴 진술이다.',
    };
  }

  const mutated = mutate(source, keywords, rand);
  if (!mutated) return null;

  return {
    no: 0,
    type: 'true_false',
    typeLabel: TYPE_LABEL.true_false,
    stem: '다음 진술이 지문의 내용과 일치하면 O, 일치하지 않으면 X를 고르시오.',
    body: mutated.text,
    choices: ['O (일치한다)', 'X (일치하지 않는다)'],
    answerIndex: 1,
    explanation: `${mutated.reason} 원문은 "${source}" 이다.`,
  };
}

/** 수치 → 서술어 → 핵심어 순으로 뒤집을 지점을 찾는다. */
function mutate(
  sentence: string,
  keywords: Keyword[],
  rand: () => number,
): { text: string; reason: string } | null {
  const num = sentence.match(/\d+(\.\d+)?/);
  if (num) {
    const original = Number(num[0]);
    // 연도를 두 배로 만들면 지문을 안 읽어도 오답인 게 보인다. 몇 해만 옮긴다.
    const isYear = Number.isInteger(original) && original >= 1900 && original <= 2100;
    const changed = isYear
      ? original + 1 + Math.floor(rand() * 6)
      : original === 0
        ? 5
        : Math.round(original * 2);
    return {
      text: sentence.replace(num[0], String(changed)),
      reason: `수치를 ${original}에서 ${changed}(으)로 바꾼 진술이다.`,
    };
  }

  for (const [pattern, replacement] of NEGATIONS) {
    if (pattern.test(sentence)) {
      pattern.lastIndex = 0;
      return {
        text: sentence.replace(pattern, replacement),
        reason: '서술을 반대로 뒤집은 진술이다.',
      };
    }
  }

  const present = keywords.find((k) => sentence.includes(k.word));
  const candidates = keywords.filter(
    (k) => k.word !== present?.word && !sentence.includes(k.word),
  );
  if (present && candidates.length > 0) {
    const other = candidates[Math.floor(rand() * candidates.length)];
    return {
      text: replaceNoun(sentence, present.word, other.word),
      reason: `${withJosa(present.word, '을')} ${withJosa(other.word, '으로')} 바꿔 놓은 진술이다.`,
    };
  }
  return null;
}

function clip(sentence: string, max = 60): string {
  return sentence.length <= max ? sentence : sentence.slice(0, max - 1) + '…';
}

/* ------------------------------------------------------------------ */
/* 조립                                                                */
/* ------------------------------------------------------------------ */

export interface GenerateOptions {
  passage: string;
  count?: number;
  types?: QuestionType[];
  seed?: string;
}

export function generateQuiz(options: GenerateOptions): Quiz {
  const { passage, count = 4, types = [...QUESTION_TYPES], seed } = options;

  const sentences = splitSentences(passage);
  if (sentences.length < 3) {
    throw new Error('지문이 너무 짧습니다. 최소 3개 문장이 필요합니다.');
  }
  const keywords = extractKeywords(passage);
  if (keywords.length < 4) {
    throw new Error('핵심어를 4개 이상 찾지 못했습니다. 더 긴 지문을 넣어 주세요.');
  }

  const rand = makeRandom(hashString(seed ?? passage));
  const questions: Question[] = [];
  const skipped: QuestionType[] = [];

  for (let i = 0; questions.length < count && i < count * 4; i++) {
    const type = types[i % types.length];
    const q =
      type === 'blank' ? makeBlank(sentences, keywords, rand)
      : type === 'order' ? makeOrder(sentences, rand)
      : type === 'main_idea' ? makeMainIdea(sentences, keywords, rand)
      : makeTrueFalse(sentences, keywords, rand);

    if (!q) {
      if (!skipped.includes(type)) skipped.push(type);
      continue;
    }
    q.no = questions.length + 1;
    questions.push(q);
  }

  if (questions.length === 0) {
    throw new Error(
      `요청한 유형(${types.join(', ')})으로 문제를 만들지 못했습니다. 지문이 짧거나 반복되는 핵심어가 부족합니다.`,
    );
  }

  return {
    quizId: `quiz_${hashString(passage + (seed ?? '')).toString(36)}`,
    createdAt: new Date().toISOString(),
    questionCount: questions.length,
    questions,
  };
}

export interface GradeResult {
  quizId: string;
  score: number;
  total: number;
  correctCount: number;
  results: {
    no: number;
    typeLabel: string;
    submitted: number | null;
    answerIndex: number;
    correct: boolean;
    explanation: string;
  }[];
}

export function gradeQuiz(quiz: Quiz, answers: number[]): GradeResult {
  const results = quiz.questions.map((q, i) => {
    const submitted = answers[i] ?? null;
    return {
      no: q.no,
      typeLabel: q.typeLabel,
      submitted,
      answerIndex: q.answerIndex,
      correct: submitted === q.answerIndex,
      explanation: q.explanation,
    };
  });
  const correctCount = results.filter((r) => r.correct).length;
  return {
    quizId: quiz.quizId,
    total: quiz.questions.length,
    correctCount,
    score: Math.round((correctCount / quiz.questions.length) * 100),
    results,
  };
}

/** 사람이 읽을 수 있는 시험지 형태로 출력한다. */
export function renderQuiz(quiz: Quiz, withAnswers: boolean): string {
  const lines: string[] = [`[문제지] ${quiz.quizId} · 총 ${quiz.questionCount}문항`, ''];

  for (const q of quiz.questions) {
    lines.push(`${q.no}. (${q.typeLabel}) ${q.stem}`);
    if (q.body) lines.push('', q.body.split('\n').map((l) => `   ${l}`).join('\n'));
    lines.push('');
    q.choices.forEach((c, i) => lines.push(`   ${'①②③④⑤'[i]} ${c}`));
    if (withAnswers) {
      lines.push('', `   ▷ 정답 ${'①②③④⑤'[q.answerIndex]} — ${q.explanation}`);
    }
    lines.push('');
  }
  return lines.join('\n').trimEnd();
}
