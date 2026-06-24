import fs from 'fs';
import { resolveSafePath } from '../security/pathGuard';
import { PROJECT_ROOT } from '../config';
import path from 'path';

export async function handleProposePatch(defect: string, targetFiles: string[]) {
  const fileContents: Record<string, string> = {};

  for (const filePath of targetFiles) {
    const safePath = resolveSafePath(filePath);
    if (!fs.existsSync(safePath)) continue;
    fileContents[filePath] = fs.readFileSync(safePath, 'utf-8');
  }

  return {
    defect,
    targetFiles,
    fileContents,
    instruction:
      '위 파일 내용을 분석하여 unified diff 형식(--- a/path, +++ b/path, @@ ... @@)으로 수정안을 작성한 뒤 apply_patch 도구로 적용하세요.',
  };
}
