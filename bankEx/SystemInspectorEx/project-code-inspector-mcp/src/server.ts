import { McpServer, ResourceTemplate } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import { PROJECT_ROOT } from './config';
import { handleIndexProject } from './tools/indexProject';
import { handleSearchCode } from './tools/searchCode';
import { handleReadFile } from './tools/readFile';
import { handleAnalyzeDefect } from './tools/analyzeDefect';
import { handleProposePatch } from './tools/proposePatch';
import { handleApplyPatch } from './tools/applyPatch';
import { handleRunTests } from './tools/runTests';
import { handleExplainPatch } from './tools/explainPatch';
import {
  getProjectSummary,
  getProjectTree,
  getFileContent,
  getSymbols,
  getRecentChanges,
} from './resources/projectResources';

const server = new McpServer({
  name: 'project-code-inspector',
  version: '1.0.0',
});

// ─── Resources ────────────────────────────────────────────────────────────────

server.resource('project-summary', 'project://summary', async () => ({
  contents: [{ uri: 'project://summary', text: await getProjectSummary(), mimeType: 'application/json' }],
}));

server.resource('project-tree', 'project://tree', async () => ({
  contents: [{ uri: 'project://tree', text: await getProjectTree(), mimeType: 'text/plain' }],
}));

server.resource(
  'project-file',
  new ResourceTemplate('project://file/{path}', { list: undefined }),
  async (uri, { path }) => ({
    contents: [{ uri: uri.href, text: await getFileContent(path as string), mimeType: 'text/plain' }],
  }),
);

server.resource('project-symbols', 'project://symbols', async () => ({
  contents: [{ uri: 'project://symbols', text: await getSymbols(), mimeType: 'application/json' }],
}));

server.resource('project-recent-changes', 'project://recent-changes', async () => ({
  contents: [{ uri: 'project://recent-changes', text: await getRecentChanges(), mimeType: 'application/json' }],
}));

// ─── Tools ────────────────────────────────────────────────────────────────────

server.tool(
  'index_project',
  '프로젝트 파일을 스캔하여 인덱스를 구축하거나 갱신합니다.',
  { force: z.boolean().default(false).describe('강제로 재인덱싱할지 여부') },
  async ({ force }) => {
    const result = await handleIndexProject(force);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  },
);

server.tool(
  'search_code',
  '키워드 또는 자연어로 프로젝트 내 관련 파일과 코드를 검색합니다.',
  { query: z.string().describe('검색할 키워드 또는 결함 설명') },
  async ({ query }) => {
    const result = await handleSearchCode(query);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  },
);

server.tool(
  'read_file',
  '프로젝트 내 특정 파일의 내용을 읽습니다.',
  { path: z.string().describe('PROJECT_ROOT 기준 상대 경로') },
  async ({ path }) => {
    const result = await handleReadFile(path);
    return { content: [{ type: 'text', text: result.content }] };
  },
);

server.tool(
  'analyze_defect',
  '결함 설명을 기반으로 관련 파일을 찾고 원인을 분석합니다.',
  { defect: z.string().describe('결함 또는 버그 설명') },
  async ({ defect }) => {
    const result = await handleAnalyzeDefect(defect);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  },
);

server.tool(
  'propose_patch',
  '수정이 필요한 파일의 내용을 제공하여 Claude가 unified diff를 생성할 수 있도록 합니다.',
  {
    defect: z.string().describe('수정할 결함 설명'),
    targetFiles: z.array(z.string()).describe('수정 대상 파일 경로 목록 (상대 경로)'),
  },
  async ({ defect, targetFiles }) => {
    const result = await handleProposePatch(defect, targetFiles);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  },
);

server.tool(
  'apply_patch',
  'unified diff를 실제 파일에 적용합니다. 적용 전 자동 백업이 생성됩니다.',
  { diff: z.string().describe('적용할 unified diff 문자열') },
  async ({ diff }) => {
    const result = await handleApplyPatch(diff);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  },
);

server.tool(
  'run_tests',
  '프로젝트 타입에 맞는 테스트를 실행합니다.',
  {
    command: z
      .string()
      .default('auto')
      .describe('"auto"로 설정하면 빌드 파일을 감지해 자동 실행. 직접 지정 시 예: "npm test"'),
  },
  async ({ command }) => {
    const result = await handleRunTests(command);
    return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
  },
);

server.tool(
  'explain_patch',
  '적용한 수정 내용을 사용자에게 설명합니다.',
  {
    diff: z.string().describe('적용된 unified diff'),
    defect: z.string().describe('수정한 결함 설명'),
    testSuccess: z.boolean().optional().describe('테스트 성공 여부'),
    testOutput: z.string().optional().describe('테스트 출력 (실패 시 원인 확인용)'),
  },
  async ({ diff, defect, testSuccess, testOutput }) => {
    const text = handleExplainPatch({
      diff,
      defect,
      testResult:
        testSuccess !== undefined
          ? { success: testSuccess, stdout: testOutput ?? '' }
          : undefined,
    });
    return { content: [{ type: 'text', text }] };
  },
);

// ─── Start ────────────────────────────────────────────────────────────────────

async function main() {
  process.stderr.write(`[project-code-inspector] PROJECT_ROOT: ${PROJECT_ROOT}\n`);
  const transport = new StdioServerTransport();
  await server.connect(transport);
  process.stderr.write('[project-code-inspector] MCP server running\n');
}

main().catch((err) => {
  process.stderr.write(`[project-code-inspector] Fatal: ${err}\n`);
  process.exit(1);
});
