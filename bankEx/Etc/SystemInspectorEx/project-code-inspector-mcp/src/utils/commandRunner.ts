import { spawn } from 'child_process';
import { DANGEROUS_COMMAND_PATTERNS, ALLOWED_TEST_COMMANDS } from '../config';

export interface CommandResult {
  success: boolean;
  exitCode: number;
  stdout: string;
  stderr: string;
}

export function assertSafeTestCommand(command: string): void {
  const base = command.split(/\s+/)[0];
  if (!ALLOWED_TEST_COMMANDS.includes(base)) {
    throw new Error(`허용되지 않은 명령: ${base}`);
  }
  for (const pattern of DANGEROUS_COMMAND_PATTERNS) {
    if (pattern.test(command)) {
      throw new Error(`위험 명령 차단: ${command}`);
    }
  }
}

export function runCommand(
  command: string,
  args: string[],
  cwd: string,
  timeoutMs = 120_000,
): Promise<CommandResult> {
  return new Promise((resolve) => {
    let stdout = '';
    let stderr = '';

    const proc = spawn(command, args, { cwd, shell: false });

    const timer = setTimeout(() => {
      proc.kill();
      resolve({ success: false, exitCode: -1, stdout, stderr: 'Timeout' });
    }, timeoutMs);

    proc.stdout.on('data', (d) => { stdout += d.toString(); });
    proc.stderr.on('data', (d) => { stderr += d.toString(); });

    proc.on('close', (code) => {
      clearTimeout(timer);
      resolve({ success: code === 0, exitCode: code ?? -1, stdout, stderr });
    });

    proc.on('error', (err) => {
      clearTimeout(timer);
      resolve({ success: false, exitCode: -1, stdout, stderr: err.message });
    });
  });
}
