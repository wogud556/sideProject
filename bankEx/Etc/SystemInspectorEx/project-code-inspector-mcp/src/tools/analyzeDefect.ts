import fs from 'fs';
import { handleSearchCode } from './searchCode';
import { getIndex } from '../indexer/projectIndexer';

export async function handleAnalyzeDefect(defect: string) {
  const { matches } = await handleSearchCode(defect);
  const relatedFiles = matches.slice(0, 5).map((m) => m.path);

  const index = getIndex();
  const fileSnippets: Record<string, string> = {};

  if (index) {
    for (const rel of relatedFiles) {
      const info = index.files.find((f) => f.relativePath === rel);
      if (info) {
        try {
          const content = fs.readFileSync(info.path, 'utf-8');
          fileSnippets[rel] = content.substring(0, 3000);
        } catch {}
      }
    }
  }

  return {
    defect,
    relatedFiles,
    suspectedCause: relatedFiles.length > 0
      ? `${relatedFiles.length}개 파일에서 관련 코드를 발견했습니다. propose_patch 도구로 수정안을 생성하세요.`
      : '관련 파일을 찾지 못했습니다. search_code로 더 구체적인 키워드를 검색해보세요.',
    fileSnippets,
  };
}
