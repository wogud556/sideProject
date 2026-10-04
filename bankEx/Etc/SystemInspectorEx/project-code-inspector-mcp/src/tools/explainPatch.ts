import { parsePatch } from 'diff';

interface ExplainInput {
  diff: string;
  defect: string;
  testResult?: { success: boolean; stdout: string };
}

export function handleExplainPatch({ diff, defect, testResult }: ExplainInput): string {
  const parsed = parsePatch(diff);
  const fileList = parsed
    .map((f) => `- ${(f.newFileName ?? f.oldFileName ?? '').replace(/^[ab]\//, '')}`)
    .join('\n');

  const additions = parsed.reduce((acc, f) => acc + f.hunks.reduce((a, h) => a + h.lines.filter(l => l.startsWith('+')).length, 0), 0);
  const deletions = parsed.reduce((acc, f) => acc + f.hunks.reduce((a, h) => a + h.lines.filter(l => l.startsWith('-')).length, 0), 0);

  const testSummary = testResult
    ? testResult.success
      ? '✅ 테스트 통과'
      : `❌ 테스트 실패\n\`\`\`\n${testResult.stdout.slice(0, 500)}\n\`\`\``
    : '테스트 미실행';

  return [
    `## 수정 내용 설명`,
    ``,
    `**결함:** ${defect}`,
    ``,
    `**수정 파일:**`,
    fileList,
    ``,
    `**변경 규모:** +${additions}줄 / -${deletions}줄`,
    ``,
    `**테스트 결과:** ${testSummary}`,
  ].join('\n');
}
