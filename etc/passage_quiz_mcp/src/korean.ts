/**
 * 형태소 분석기 없이 한국어 지문을 다루기 위한 최소 유틸.
 * 어절에서 조사를 떼어내는 휴리스틱에 의존하므로 완벽하지 않다.
 */

/** 길이가 긴 것부터 매칭해야 "에서는"이 "는"보다 먼저 잡힌다. */
const JOSA = [
  '으로부터', '로부터', '으로써', '으로서', '에서는', '에게서', '한테서', '이라고',
  '에게는', '에서도', '까지도', '조차도', '만큼은', '와의', '과의', '라고',
  '에서', '에게', '한테', '으로', '이나', '든지', '까지', '부터', '처럼',
  '보다', '만큼', '대로', '조차', '마저', '라도', '이며', '에는', '에도',
  '와', '과', '은', '는', '이', '가', '을', '를', '의', '에', '도', '만', '로', '랑', '며',
];

/** 어미로 끝나는 어절(용언)은 키워드 후보에서 제외한다. */
const PREDICATE_ENDINGS = ['하는', '하며', '하고', '하여', '해서', '되어', '되는'];

/** 조사 한 글자로 끝나는 두 글자 어절은 조사를 뗄 수 없어 그대로 두면 노이즈가 된다. */
const SINGLE_JOSA = new Set(['을', '를', '은', '는', '이', '가', '의', '에', '도', '만', '로']);

const STOPWORDS = new Set([
  '그리고', '그러나', '하지만', '그런데', '따라서', '그래서', '또한', '즉',
  '이러한', '그러한', '이런', '그런', '어떤', '모든', '많은', '다른',
  '때문', '것이', '것은', '것을', '수가', '경우', '대한', '통해', '위해',
  '가장', '매우', '더욱', '역시', '다시', '거의', '바로', '함께',
]);

/** 문장 부호 뒤 공백을 기준으로 문장을 나눈다. 소수점(3.5)은 뒤에 공백이 없어 안전하다. */
export function splitSentences(text: string): string[] {
  return text
    .split(/(?<=[.!?][”"’')\]]?)\s+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

export function splitParagraphs(text: string): string[] {
  return text
    .split(/\n\s*\n/)
    .map((p) => p.trim().replace(/\s*\n\s*/g, ' '))
    .filter((p) => p.length > 0);
}

/** 어절에서 조사를 제거한다. 남는 글자가 2자 미만이면 원형을 유지한다. */
export function stripJosa(word: string): string {
  for (const josa of JOSA) {
    if (word.length - josa.length >= 2 && word.endsWith(josa)) {
      return word.slice(0, -josa.length);
    }
  }
  return word;
}

/** 문장 부호를 떼고 어절로 자른다. */
export function toEojeol(sentence: string): string[] {
  return sentence
    .replace(/[.,!?“”"''‘’()\[\]·…]/g, ' ')
    .split(/\s+/)
    .filter(Boolean);
}

function isKeywordCandidate(word: string): boolean {
  if (word.length < 2) return false;
  if (!/^[가-힣]+$/.test(word)) return false;
  if (STOPWORDS.has(word)) return false;
  // 한국어 서술어는 사실상 모두 '다'로 끝난다. 2자 명사('바다')는 살리려고 3자부터 자른다.
  if (word.length >= 3 && word.endsWith('다')) return false;
  if (PREDICATE_ENDINGS.some((e) => word.endsWith(e))) return false;
  // '열을'처럼 조사를 떼면 1자만 남아 stripJosa 가 포기한 어절은 버린다.
  if (word.length === 2 && SINGLE_JOSA.has(word[1])) return false;
  return true;
}

export interface Keyword {
  word: string;
  count: number;
}

/** 빈도순 핵심어. 동점이면 먼저 등장한 쪽이 앞선다. */
export function extractKeywords(text: string, limit = 12): Keyword[] {
  const counts = new Map<string, number>();
  const firstSeen = new Map<string, number>();
  let position = 0;

  for (const eojeol of toEojeol(text)) {
    const stem = stripJosa(eojeol);
    position += 1;
    if (!isKeywordCandidate(stem)) continue;
    counts.set(stem, (counts.get(stem) ?? 0) + 1);
    if (!firstSeen.has(stem)) firstSeen.set(stem, position);
  }

  return [...counts.entries()]
    .map(([word, count]) => ({ word, count }))
    .sort((a, b) =>
      b.count - a.count || firstSeen.get(a.word)! - firstSeen.get(b.word)!,
    )
    .slice(0, limit);
}

/** 마지막 글자에 받침이 있는지. 한글이 아니면 받침이 있는 것으로 본다. */
export function hasBatchim(word: string): boolean {
  const code = word.charCodeAt(word.length - 1);
  if (code < 0xac00 || code > 0xd7a3) return true;
  return (code - 0xac00) % 28 !== 0;
}

/** 받침 유무에 맞는 조사 표기를 고른다. */
const JOSA_PAIRS: Record<string, [withBatchim: string, withoutBatchim: string]> = {
  이: ['이', '가'], 가: ['이', '가'],
  은: ['은', '는'], 는: ['은', '는'],
  을: ['을', '를'], 를: ['을', '를'],
  과: ['과', '와'], 와: ['과', '와'],
  으로: ['으로', '로'], 로: ['으로', '로'],
};

export function withJosa(word: string, josa: string): string {
  const pair = JOSA_PAIRS[josa];
  if (!pair) return word + josa;
  // '로/으로'는 ㄹ 받침 뒤에서 '로'를 쓴다.
  if ((josa === '로' || josa === '으로') && word.endsWith('ᆯ')) return word + '로';
  const code = word.charCodeAt(word.length - 1);
  if ((josa === '로' || josa === '으로') && code >= 0xac00 && code <= 0xd7a3
      && (code - 0xac00) % 28 === 8) {
    return word + '로';
  }
  return word + (hasBatchim(word) ? pair[0] : pair[1]);
}

/** 문장 안의 명사를 갈아 끼우면서 뒤따르는 조사를 새 명사에 맞게 고친다. */
export function replaceNoun(sentence: string, from: string, to: string): string {
  const josaAlt = Object.keys(JOSA_PAIRS).sort((a, b) => b.length - a.length).join('|');
  const pattern = new RegExp(`${escapeRegExp(from)}(${josaAlt})?`, 'g');
  return sentence.replace(pattern, (_m, josa?: string) =>
    josa ? withJosa(to, josa) : to,
  );
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** 어절 수 / 문장당 평균 어절 수로 매긴 대략적인 난이도. */
export function estimateDifficulty(text: string): {
  level: '쉬움' | '보통' | '어려움';
  avgEojeolPerSentence: number;
} {
  const sentences = splitSentences(text);
  const eojeol = toEojeol(text).length;
  const avg = sentences.length ? eojeol / sentences.length : 0;
  const level = avg < 12 ? '쉬움' : avg < 18 ? '보통' : '어려움';
  return { level, avgEojeolPerSentence: Number(avg.toFixed(1)) };
}
