import fg from 'fast-glob';
import fs from 'fs';
import path from 'path';
import { PROJECT_ROOT, SOURCE_EXTENSIONS, EXCLUDE_PATTERNS } from '../config';
import { extractSymbols, SymbolInfo } from './symbolExtractor';

export interface FileInfo {
  path: string;
  relativePath: string;
  size: number;
  ext: string;
  lastModified: Date;
  symbols: SymbolInfo[];
}

export interface ProjectIndex {
  root: string;
  files: FileInfo[];
  indexedAt: Date;
  languages: string[];
  skipped: number;
}

let currentIndex: ProjectIndex | null = null;

export async function indexProject(force = false): Promise<ProjectIndex> {
  if (currentIndex && !force) return currentIndex;

  const patterns = SOURCE_EXTENSIONS.map((ext) => `**/*${ext}`);
  const absolutePaths = await fg(patterns, {
    cwd: PROJECT_ROOT,
    ignore: EXCLUDE_PATTERNS,
    absolute: true,
    onlyFiles: true,
  });

  const files: FileInfo[] = [];
  const langSet = new Set<string>();
  let skipped = 0;

  for (const filePath of absolutePaths) {
    try {
      const stat = fs.statSync(filePath);
      if (stat.size > 1_000_000) { skipped++; continue; } // skip files > 1MB

      const ext = path.extname(filePath);
      const content = fs.readFileSync(filePath, 'utf-8');
      const symbols = extractSymbols(content, ext);
      const relativePath = path.relative(PROJECT_ROOT, filePath);

      langSet.add(ext.replace('.', '').toUpperCase());
      files.push({ path: filePath, relativePath, size: stat.size, ext, lastModified: stat.mtime, symbols });
    } catch {
      skipped++;
    }
  }

  currentIndex = { root: PROJECT_ROOT, files, indexedAt: new Date(), languages: Array.from(langSet), skipped };
  return currentIndex;
}

export function getIndex(): ProjectIndex | null {
  return currentIndex;
}

export function buildFileTree(files: FileInfo[]): string {
  const tree: Record<string, unknown> = {};

  for (const file of files) {
    const parts = file.relativePath.split(path.sep);
    let node = tree as Record<string, unknown>;
    for (const part of parts) {
      if (!node[part]) node[part] = {};
      node = node[part] as Record<string, unknown>;
    }
  }

  function render(obj: Record<string, unknown>, prefix = ''): string {
    const entries = Object.entries(obj);
    return entries
      .map(([key, children], i) => {
        const isLast = i === entries.length - 1;
        const conn = isLast ? '└─ ' : '├─ ';
        const childPfx = prefix + (isLast ? '   ' : '│  ');
        const childStr =
          children && Object.keys(children as object).length > 0
            ? '\n' + render(children as Record<string, unknown>, childPfx)
            : '';
        return `${prefix}${conn}${key}${childStr}`;
      })
      .join('\n');
  }

  return render(tree);
}
