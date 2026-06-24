import fs from 'fs';

export function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf-8');
}

export function writeFile(filePath: string, content: string): void {
  fs.writeFileSync(filePath, content, 'utf-8');
}

export function backupFile(filePath: string): string {
  const backupPath = `${filePath}.bak.${Date.now()}`;
  fs.copyFileSync(filePath, backupPath);
  return backupPath;
}

export function restoreBackup(backupPath: string, originalPath: string): void {
  fs.copyFileSync(backupPath, originalPath);
  fs.unlinkSync(backupPath);
}

export function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath);
}
