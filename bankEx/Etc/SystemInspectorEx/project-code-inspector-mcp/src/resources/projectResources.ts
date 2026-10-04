import fs from 'fs';
import path from 'path';
import { indexProject, buildFileTree } from '../indexer/projectIndexer';
import { resolveSafePath } from '../security/pathGuard';
import { PROJECT_ROOT } from '../config';

export async function getProjectSummary(): Promise<string> {
  const index = await indexProject();
  const extCounts: Record<string, number> = {};
  for (const f of index.files) {
    extCounts[f.ext] = (extCounts[f.ext] ?? 0) + 1;
  }
  const topExts = Object.entries(extCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([ext, n]) => `${ext}(${n})`)
    .join(', ');

  return JSON.stringify(
    {
      name: path.basename(PROJECT_ROOT),
      root: PROJECT_ROOT,
      totalFiles: index.files.length,
      languages: index.languages,
      topExtensions: topExts,
      indexedAt: index.indexedAt,
    },
    null,
    2,
  );
}

export async function getProjectTree(): Promise<string> {
  const index = await indexProject();
  return buildFileTree(index.files);
}

export async function getFileContent(filePath: string): Promise<string> {
  const safePath = resolveSafePath(filePath);
  return fs.readFileSync(safePath, 'utf-8');
}

export async function getSymbols(): Promise<string> {
  const index = await indexProject();
  const symbols = index.files.flatMap((f) =>
    f.symbols.map((s) => ({ file: f.relativePath, ...s })),
  );
  return JSON.stringify(symbols, null, 2);
}

export async function getRecentChanges(): Promise<string> {
  const index = await indexProject();
  const recent = [...index.files]
    .sort((a, b) => b.lastModified.getTime() - a.lastModified.getTime())
    .slice(0, 20)
    .map((f) => ({ path: f.relativePath, lastModified: f.lastModified, sizeBytes: f.size }));
  return JSON.stringify(recent, null, 2);
}
