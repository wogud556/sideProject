import path from 'path';
import { PROJECT_ROOT, SENSITIVE_FILENAME_PATTERNS } from '../config';

export function resolveSafePath(inputPath: string): string {
  const resolved = path.isAbsolute(inputPath)
    ? path.resolve(inputPath)
    : path.resolve(PROJECT_ROOT, inputPath);

  const rootWithSep = PROJECT_ROOT.endsWith(path.sep)
    ? PROJECT_ROOT
    : PROJECT_ROOT + path.sep;

  if (resolved !== PROJECT_ROOT && !resolved.startsWith(rootWithSep)) {
    throw new Error(`접근 거부: PROJECT_ROOT 외부 경로 — ${inputPath}`);
  }

  const lower = path.basename(resolved).toLowerCase();
  for (const pattern of SENSITIVE_FILENAME_PATTERNS) {
    if (lower.includes(pattern)) {
      throw new Error(`접근 거부: 민감한 파일명 패턴 — ${path.basename(resolved)}`);
    }
  }

  return resolved;
}
