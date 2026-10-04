import { indexProject } from '../indexer/projectIndexer';

export async function handleIndexProject(force: boolean) {
  const index = await indexProject(force);
  return {
    indexedFiles: index.files.length,
    skippedFiles: index.skipped,
    languages: index.languages,
    indexedAt: index.indexedAt.toISOString(),
  };
}
