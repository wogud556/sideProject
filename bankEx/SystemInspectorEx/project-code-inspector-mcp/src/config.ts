import path from 'path';

export const PROJECT_ROOT = process.env.PROJECT_ROOT
  ? path.resolve(process.env.PROJECT_ROOT)
  : process.cwd();

export const SOURCE_EXTENSIONS = [
  '.java', '.kt', '.js', '.ts', '.tsx', '.jsx',
  '.py', '.go', '.rb', '.sql', '.xml', '.yml',
  '.yaml', '.json', '.md',
];

export const EXCLUDE_PATTERNS = [
  '**/node_modules/**',
  '**/.gradle/**',
  '**/.git/**',
  '**/build/**',
  '**/dist/**',
  '**/target/**',
  '**/out/**',
  '**/.idea/**',
  '**/.vscode/**',
  '**/*.class',
  '**/*.jar',
  '**/*.war',
  '**/*.log',
  '**/.env',
];

export const SENSITIVE_FILENAME_PATTERNS = [
  '.env', 'secret', 'credential', 'password', 'passwd', 'private', '.pem', '.key',
];

export const DANGEROUS_COMMAND_PATTERNS = [
  /\brm\b/, /\bsudo\b/, /curl.*\|.*sh/, /chmod\s+777/, /\bdd\b/, /\bmkfs\b/,
  /\bdrop\s+table\b/i, /\btruncate\b/i,
];

export const ALLOWED_TEST_COMMANDS = [
  './gradlew', 'mvn', 'mvnw', './mvnw', 'npm', 'yarn', 'go', 'bundle', 'pytest', 'cargo',
];
