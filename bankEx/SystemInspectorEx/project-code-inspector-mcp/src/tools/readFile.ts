import fs from 'fs';
import { resolveSafePath } from '../security/pathGuard';
import { PROJECT_ROOT } from '../config';
import path from 'path';

export async function handleReadFile(filePath: string): Promise<{ path: string; content: string; lines: number }> {
  const safePath = resolveSafePath(filePath);

  if (!fs.existsSync(safePath)) {
    throw new Error(`파일을 찾을 수 없습니다: ${filePath}`);
  }

  const content = fs.readFileSync(safePath, 'utf-8');
  return {
    path: path.relative(PROJECT_ROOT, safePath),
    content,
    lines: content.split('\n').length,
  };
}
