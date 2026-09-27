/**
 * 서버를 stdio로 띄우고 실제 MCP 클라이언트로 한 바퀴 돌려 보는 스모크 테스트.
 *   node scripts/smoke.mjs
 */
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const passage = readFileSync(join(ROOT, 'samples', 'passage-ko.txt'), 'utf-8');

const transport = new StdioClientTransport({
  command: process.execPath,
  args: [join(ROOT, 'dist', 'index.js')],
});
const client = new Client({ name: 'smoke', version: '0.0.0' });
await client.connect(transport);

const line = (t) => console.log('\n' + '─'.repeat(64) + '\n' + t + '\n');

line('tools / prompts / resources');
console.log('tools    :', (await client.listTools()).tools.map((t) => t.name).join(', '));
console.log('prompts  :', (await client.listPrompts()).prompts.map((p) => p.name).join(', '));
console.log('resources:', (await client.listResources()).resources.map((r) => r.uri).join(', '));

line('resources/read — 샘플 지문');
const res = await client.readResource({ uri: 'passage://sample/ko-1' });
console.log(res.contents[0].text.slice(0, 60) + '…');

line('analyze_passage');
const analyzed = await client.callTool({ name: 'analyze_passage', arguments: { passage } });
console.log(analyzed.content[0].text);

line('generate_quiz — 4문항, seed 고정');
const quiz = await client.callTool({
  name: 'generate_quiz',
  arguments: { passage, count: 4, seed: 'demo', includeAnswers: true },
});
console.log(quiz.content[0].text);

const quizId = quiz.content[0].text.match(/quizId: (\S+)/)[1];

line('generate_quiz — 같은 seed 재현성 확인');
const again = await client.callTool({
  name: 'generate_quiz',
  arguments: { passage, count: 4, seed: 'demo', includeAnswers: true },
});
console.log(again.content[0].text === quiz.content[0].text ? '동일한 문제지 생성 OK' : '재현 실패');

line('grade_quiz — 전부 1번으로 찍었을 때');
const graded = await client.callTool({
  name: 'grade_quiz',
  arguments: { quizId, answers: [1, 1, 1, 1] },
});
console.log(graded.content[0].text);
console.log('structuredContent:', graded.structuredContent);

line('prompts/get — quiz_from_passage');
const prompt = await client.getPrompt({
  name: 'quiz_from_passage',
  arguments: { passage: passage.slice(0, 120), grade: '중3', count: '3' },
});
console.log(prompt.messages[0].content.text.slice(0, 260) + '…');

line('오류 처리 — 없는 quizId');
const bad = await client.callTool({ name: 'grade_quiz', arguments: { quizId: 'nope', answers: [1] } });
console.log('isError:', bad.isError, '|', bad.content[0].text.slice(0, 60) + '…');

await client.close();
console.log('\n스모크 테스트 통과\n');
