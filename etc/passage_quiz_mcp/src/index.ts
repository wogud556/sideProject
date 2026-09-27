#!/usr/bin/env node
/**
 * passage-quiz MCP 서버
 *
 * 지문(독해 텍스트)을 넣으면 문제를 만들어 주는 MCP 서버 샘플.
 *
 *  - Tools    : 지문 분석 / 문제 생성 / 채점  (LLM 없이 규칙 기반으로 동작)
 *  - Prompts  : 호스트 LLM에게 고품질 출제를 맡기는 경로
 *  - Resources: 테스트용 샘플 지문
 */
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import { estimateDifficulty, extractKeywords, splitParagraphs, splitSentences } from './korean.js';
import {
  QUESTION_TYPES,
  generateQuiz,
  gradeQuiz,
  renderQuiz,
  type Quiz,
  type QuestionType,
} from './quiz.js';

const HERE = dirname(fileURLToPath(import.meta.url));

/** 채점 때 다시 꺼내 쓰려고 생성한 문제지를 들고 있는다. 프로세스가 살아 있는 동안만 유효하다. */
const quizStore = new Map<string, Quiz>();
const MAX_STORED = 50;

function rememberQuiz(quiz: Quiz): void {
  if (quizStore.size >= MAX_STORED) {
    quizStore.delete(quizStore.keys().next().value!);
  }
  quizStore.set(quiz.quizId, quiz);
}

const server = new McpServer({ name: 'passage-quiz', version: '0.1.0' });

/* ------------------------------------------------------------------ */
/* Tool 1 — 지문 분석                                                   */
/* ------------------------------------------------------------------ */

server.registerTool(
  'analyze_passage',
  {
    title: '지문 분석',
    description:
      '지문의 문단·문장 수, 핵심어, 대략적인 난이도를 계산한다. 어떤 유형의 문제를 낼 수 있을지 먼저 가늠할 때 쓴다.',
    inputSchema: {
      passage: z.string().min(30).describe('분석할 지문 원문'),
    },
    outputSchema: {
      paragraphCount: z.number(),
      sentenceCount: z.number(),
      charCount: z.number(),
      difficulty: z.string(),
      avgEojeolPerSentence: z.number(),
      keywords: z.array(z.object({ word: z.string(), count: z.number() })),
    },
    annotations: { readOnlyHint: true },
  },
  async ({ passage }) => {
    const sentences = splitSentences(passage);
    const { level, avgEojeolPerSentence } = estimateDifficulty(passage);
    const structured = {
      paragraphCount: splitParagraphs(passage).length,
      sentenceCount: sentences.length,
      charCount: passage.length,
      difficulty: level,
      avgEojeolPerSentence,
      keywords: extractKeywords(passage, 10),
    };

    const text = [
      `문단 ${structured.paragraphCount}개 · 문장 ${structured.sentenceCount}개 · ${structured.charCount}자`,
      `난이도 ${level} (문장당 평균 ${avgEojeolPerSentence}어절)`,
      `핵심어: ${structured.keywords.map((k) => `${k.word}(${k.count})`).join(', ')}`,
    ].join('\n');

    return { content: [{ type: 'text', text }], structuredContent: structured };
  },
);

/* ------------------------------------------------------------------ */
/* Tool 2 — 문제 생성                                                   */
/* ------------------------------------------------------------------ */

server.registerTool(
  'generate_quiz',
  {
    title: '지문으로 문제 만들기',
    description:
      '지문에서 객관식 문제를 만든다. 빈칸 추론·문장 배열·중심 내용·사실 확인(O/X) 네 가지 유형을 규칙 기반으로 생성하므로 LLM 호출이 필요 없다. 같은 지문과 같은 seed는 항상 같은 문제를 낸다.',
    inputSchema: {
      passage: z.string().min(30).describe('출제할 지문 원문'),
      count: z.number().int().min(1).max(10).default(4).describe('문항 수'),
      types: z
        .array(z.enum(QUESTION_TYPES))
        .optional()
        .describe('출제할 유형. 생략하면 네 유형을 번갈아 낸다.'),
      seed: z.string().optional().describe('같은 문제를 재현하려면 고정 문자열을 넣는다.'),
      includeAnswers: z.boolean().default(true).describe('정답과 해설을 함께 낼지 여부'),
    },
    annotations: { readOnlyHint: true },
  },
  async ({ passage, count, types, seed, includeAnswers }) => {
    try {
      const quiz = generateQuiz({
        passage,
        count,
        types: types as QuestionType[] | undefined,
        seed,
      });
      rememberQuiz(quiz);

      const text = [
        renderQuiz(quiz, includeAnswers),
        '',
        `※ quizId: ${quiz.quizId} — grade_quiz 로 채점할 때 이 값을 쓴다.`,
      ].join('\n');

      return { content: [{ type: 'text', text }] };
    } catch (error) {
      return {
        isError: true,
        content: [{ type: 'text', text: `문제를 만들지 못했습니다: ${(error as Error).message}` }],
      };
    }
  },
);

/* ------------------------------------------------------------------ */
/* Tool 3 — 채점                                                        */
/* ------------------------------------------------------------------ */

server.registerTool(
  'grade_quiz',
  {
    title: '채점',
    description:
      'generate_quiz 가 만든 문제지에 대한 답안을 채점한다. 답안은 1번 문항부터 순서대로, 선택지 번호(1부터 시작)를 배열로 넣는다.',
    inputSchema: {
      quizId: z.string().describe('generate_quiz 가 돌려준 quizId'),
      answers: z.array(z.number().int().min(1).max(5)).describe('문항 순서대로의 선택지 번호'),
    },
    outputSchema: {
      quizId: z.string(),
      score: z.number(),
      total: z.number(),
      correctCount: z.number(),
    },
    annotations: { readOnlyHint: true },
  },
  async ({ quizId, answers }) => {
    const quiz = quizStore.get(quizId);
    if (!quiz) {
      return {
        isError: true,
        content: [
          {
            type: 'text',
            text: `quizId '${quizId}' 를 찾을 수 없습니다. 문제지는 서버가 실행 중인 동안만 보관되니 generate_quiz 를 다시 호출해 주세요.`,
          },
        ],
      };
    }

    const result = gradeQuiz(quiz, answers.map((a) => a - 1));
    const detail = result.results
      .map((r) => {
        const mark = r.correct ? 'O' : 'X';
        const submitted = r.submitted === null ? '미응답' : `${r.submitted + 1}번`;
        return `${r.no}. ${mark} (${r.typeLabel}) 제출 ${submitted} / 정답 ${r.answerIndex + 1}번\n   ${r.explanation}`;
      })
      .join('\n');

    return {
      content: [
        {
          type: 'text',
          text: `${result.correctCount}/${result.total} 정답 · ${result.score}점\n\n${detail}`,
        },
      ],
      structuredContent: {
        quizId: result.quizId,
        score: result.score,
        total: result.total,
        correctCount: result.correctCount,
      },
    };
  },
);

/* ------------------------------------------------------------------ */
/* Prompts — 규칙 기반으로는 한계가 있는 출제를 호스트 LLM에 넘긴다.      */
/* ------------------------------------------------------------------ */

server.registerPrompt(
  'quiz_from_passage',
  {
    title: '지문으로 문제 출제 (LLM)',
    description:
      'generate_quiz 의 규칙 기반 출제로는 어려운 추론·어휘·서술형 문제를, 호스트의 LLM에게 직접 맡기는 프롬프트.',
    argsSchema: {
      passage: z.string().describe('출제할 지문'),
      grade: z.string().optional().describe('대상 학년 (예: 중3, 고1)'),
      count: z.string().optional().describe('문항 수'),
    },
  },
  ({ passage, grade, count }) => ({
    messages: [
      {
        role: 'user',
        content: {
          type: 'text',
          text: [
            `다음 지문으로 ${grade ?? '고등학교 1학년'} 수준의 독해 문제 ${count ?? '4'}문항을 출제해 주세요.`,
            '',
            '조건:',
            '- 유형을 섞을 것 — 중심 내용, 세부 정보 확인, 추론, 어휘의 문맥적 의미',
            '- 객관식 4지선다. 오답 선택지도 지문을 읽지 않으면 고를 수 없을 만큼 그럴듯하게 만들 것',
            '- 지문에 근거가 없는 내용을 정답으로 삼지 말 것',
            '- 각 문항마다 정답의 근거가 되는 지문 문장을 그대로 인용할 것',
            '',
            '[지문]',
            passage,
          ].join('\n'),
        },
      },
    ],
  }),
);

server.registerPrompt(
  'explain_answer',
  {
    title: '오답 해설 작성',
    description: '학생이 틀린 문항에 대해, 지문 근거를 짚어 주는 해설을 작성하게 하는 프롬프트.',
    argsSchema: {
      passage: z.string().describe('원 지문'),
      question: z.string().describe('문항 전문 (발문 + 선택지)'),
      studentAnswer: z.string().describe('학생이 고른 답'),
      correctAnswer: z.string().describe('정답'),
    },
  },
  ({ passage, question, studentAnswer, correctAnswer }) => ({
    messages: [
      {
        role: 'user',
        content: {
          type: 'text',
          text: [
            '학생이 아래 문항을 틀렸습니다. 해설을 써 주세요.',
            '',
            `학생 답: ${studentAnswer} / 정답: ${correctAnswer}`,
            '',
            '해설에는 다음을 담아 주세요.',
            '1. 정답의 근거가 되는 지문 문장 인용',
            '2. 학생이 고른 선택지가 왜 틀렸는지 — 지문의 어느 부분과 어긋나는지',
            '3. 같은 실수를 반복하지 않으려면 무엇을 확인해야 하는지 한 문장',
            '',
            '[문항]',
            question,
            '',
            '[지문]',
            passage,
          ].join('\n'),
        },
      },
    ],
  }),
);

/* ------------------------------------------------------------------ */
/* Resource — 바로 실험해 볼 수 있는 샘플 지문                           */
/* ------------------------------------------------------------------ */

server.registerResource(
  'sample-passage',
  'passage://sample/ko-1',
  {
    title: '샘플 지문 (도시 열섬 현상)',
    description: '도구를 바로 시험해 볼 수 있는 설명문 지문.',
    mimeType: 'text/plain',
  },
  async (uri) => ({
    contents: [
      {
        uri: uri.href,
        mimeType: 'text/plain',
        text: readFileSync(join(HERE, '..', 'samples', 'passage-ko.txt'), 'utf-8'),
      },
    ],
  }),
);

/* ------------------------------------------------------------------ */

const transport = new StdioServerTransport();
await server.connect(transport);
console.error('passage-quiz MCP server ready (stdio)');
