export interface SymbolInfo {
  type: 'class' | 'function' | 'interface' | 'import';
  name: string;
  line: number;
}

const LANG_PATTERNS: Record<string, Array<{ type: SymbolInfo['type']; re: RegExp }>> = {
  java: [
    { type: 'class',    re: /^(?:public\s+)?(?:class|interface|enum)\s+(\w+)/m },
    { type: 'function', re: /^\s+(?:public|private|protected)\s+\w[\w<>, ]*\s+(\w+)\s*\(/mg },
    { type: 'import',   re: /^import\s+([\w.]+);/mg },
  ],
  kt: [
    { type: 'class',    re: /^(?:data\s+)?(?:class|object|interface)\s+(\w+)/m },
    { type: 'function', re: /^\s*(?:override\s+)?fun\s+(\w+)\s*\(/mg },
    { type: 'import',   re: /^import\s+([\w.]+)/mg },
  ],
  ts: [
    { type: 'class',     re: /^(?:export\s+)?(?:abstract\s+)?class\s+(\w+)/mg },
    { type: 'interface', re: /^(?:export\s+)?interface\s+(\w+)/mg },
    { type: 'function',  re: /^(?:export\s+)?(?:async\s+)?function\s+(\w+)/mg },
    { type: 'function',  re: /^(?:export\s+)?const\s+(\w+)\s*=\s*(?:async\s*)?\(/mg },
    { type: 'import',    re: /^import\s+.+from\s+['"](.+)['"]/mg },
  ],
  tsx: [
    { type: 'class',    re: /^(?:export\s+)?class\s+(\w+)/mg },
    { type: 'function', re: /^(?:export\s+)?(?:async\s+)?function\s+(\w+)/mg },
    { type: 'function', re: /^(?:export\s+)?const\s+(\w+)\s*=\s*(?:async\s*)?\(/mg },
    { type: 'import',   re: /^import\s+.+from\s+['"](.+)['"]/mg },
  ],
  js: [
    { type: 'function', re: /^(?:export\s+)?(?:async\s+)?function\s+(\w+)/mg },
    { type: 'function', re: /^(?:export\s+)?const\s+(\w+)\s*=\s*(?:async\s*)?\(/mg },
    { type: 'import',   re: /^(?:import|require)\s*\(?['"](.+)['"]\)?/mg },
  ],
  py: [
    { type: 'class',    re: /^class\s+(\w+)/mg },
    { type: 'function', re: /^def\s+(\w+)/mg },
    { type: 'import',   re: /^(?:from\s+\S+\s+)?import\s+(\S+)/mg },
  ],
  go: [
    { type: 'function', re: /^func\s+(?:\(\w+\s+\*?\w+\)\s+)?(\w+)\s*\(/mg },
    { type: 'class',    re: /^type\s+(\w+)\s+struct/mg },
    { type: 'import',   re: /^\s+"([\w./]+)"/mg },
  ],
};

export function extractSymbols(content: string, ext: string): SymbolInfo[] {
  const lang = ext.replace('.', '');
  const patterns = LANG_PATTERNS[lang];
  if (!patterns) return [];

  const symbols: SymbolInfo[] = [];

  for (const { type, re } of patterns) {
    const globalRe = new RegExp(re.source, re.flags.includes('g') ? re.flags : re.flags + 'g');
    let match: RegExpExecArray | null;
    while ((match = globalRe.exec(content)) !== null) {
      const lineNum = content.substring(0, match.index).split('\n').length;
      symbols.push({ type, name: match[1] ?? match[0], line: lineNum });
    }
  }

  return symbols;
}
