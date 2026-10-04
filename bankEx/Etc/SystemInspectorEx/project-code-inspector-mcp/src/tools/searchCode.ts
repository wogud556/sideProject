import fs from 'fs';
import { indexProject } from '../indexer/projectIndexer';

interface SearchMatch {
  path: string;
  score: number;
  snippet: string;
  matchedSymbols: string[];
}

export async function handleSearchCode(query: string): Promise<{ matches: SearchMatch[] }> {
  const index = await indexProject();
  const keywords = query.toLowerCase().split(/\s+/).filter((k) => k.length > 1);

  const results: SearchMatch[] = [];

  for (const file of index.files) {
    try {
      const content = fs.readFileSync(file.path, 'utf-8');
      const lower = content.toLowerCase();

      let contentScore = 0;
      let bestSnippet = '';

      for (const kw of keywords) {
        const idx = lower.indexOf(kw);
        if (idx !== -1) {
          contentScore += 1 / keywords.length;
          if (!bestSnippet) {
            const start = Math.max(0, idx - 60);
            const end = Math.min(content.length, idx + 200);
            bestSnippet = content.substring(start, end).replace(/\n{3,}/g, '\n\n').trim();
          }
        }
      }

      const matchedSymbols = file.symbols
        .filter((s) => keywords.some((kw) => s.name.toLowerCase().includes(kw)))
        .map((s) => `${s.type}:${s.name}`);

      const symbolScore = matchedSymbols.length * 0.3;
      const totalScore = Math.min(1, contentScore + symbolScore);

      if (totalScore > 0) {
        results.push({
          path: file.relativePath,
          score: Math.round(totalScore * 100) / 100,
          snippet: bestSnippet,
          matchedSymbols,
        });
      }
    } catch {
      // skip unreadable
    }
  }

  results.sort((a, b) => b.score - a.score);
  return { matches: results.slice(0, 10) };
}
