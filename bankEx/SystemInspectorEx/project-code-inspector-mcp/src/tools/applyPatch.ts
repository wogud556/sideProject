import fs from 'fs';
import { applyPatch, parsePatch } from 'diff';
import { resolveSafePath } from '../security/pathGuard';
import { backupFile, restoreBackup } from '../utils/fileUtils';
import { runCommand } from '../utils/commandRunner';
import { PROJECT_ROOT } from '../config';

interface ApplyResult {
  success: boolean;
  appliedFiles: string[];
  backups: Record<string, string>;
  gitDiff: string;
  error?: string;
}

export async function handleApplyPatch(diff: string): Promise<ApplyResult> {
  const parsedFiles = parsePatch(diff);
  if (parsedFiles.length === 0) {
    return { success: false, appliedFiles: [], backups: {}, gitDiff: '', error: 'diff에서 파일을 파싱할 수 없습니다.' };
  }

  const backups: Record<string, string> = {};
  const appliedFiles: string[] = [];

  try {
    for (const patchedFile of parsedFiles) {
      // parsePatch returns oldFileName with 'a/' prefix
      const rawPath = (patchedFile.oldFileName ?? '').replace(/^a\//, '');
      if (!rawPath) continue;

      const safePath = resolveSafePath(rawPath);

      if (!fs.existsSync(safePath)) {
        throw new Error(`대상 파일 없음: ${rawPath}`);
      }

      const original = fs.readFileSync(safePath, 'utf-8');
      const backupPath = backupFile(safePath);
      backups[rawPath] = backupPath;

      const patched = applyPatch(original, diff);
      if (patched === false) {
        throw new Error(`패치 적용 실패: ${rawPath} — 파일 내용과 diff가 일치하지 않습니다.`);
      }

      fs.writeFileSync(safePath, patched, 'utf-8');
      appliedFiles.push(rawPath);
    }

    const gitResult = await runCommand('git', ['diff'], PROJECT_ROOT);
    return { success: true, appliedFiles, backups, gitDiff: gitResult.stdout };

  } catch (err: unknown) {
    for (const [rel, bak] of Object.entries(backups)) {
      try {
        const safePath = resolveSafePath(rel);
        restoreBackup(bak, safePath);
      } catch {}
    }
    return {
      success: false,
      appliedFiles: [],
      backups: {},
      gitDiff: '',
      error: err instanceof Error ? err.message : String(err),
    };
  }
}
