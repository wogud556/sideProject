import fs from 'fs';
import path from 'path';
import { runCommand, assertSafeTestCommand } from '../utils/commandRunner';
import { PROJECT_ROOT } from '../config';

interface TestCommand { cmd: string; args: string[] }

function detectTestCommand(): TestCommand | null {
  if (fs.existsSync(path.join(PROJECT_ROOT, 'build.gradle')) || fs.existsSync(path.join(PROJECT_ROOT, 'build.gradle.kts'))) {
    return { cmd: './gradlew', args: ['test'] };
  }
  if (fs.existsSync(path.join(PROJECT_ROOT, 'pom.xml'))) {
    const mvnw = path.join(PROJECT_ROOT, 'mvnw');
    return { cmd: fs.existsSync(mvnw) ? './mvnw' : 'mvn', args: ['test'] };
  }
  if (fs.existsSync(path.join(PROJECT_ROOT, 'package.json'))) {
    return { cmd: 'npm', args: ['test'] };
  }
  if (fs.existsSync(path.join(PROJECT_ROOT, 'go.mod'))) {
    return { cmd: 'go', args: ['test', './...'] };
  }
  if (fs.existsSync(path.join(PROJECT_ROOT, 'Gemfile'))) {
    return { cmd: 'bundle', args: ['exec', 'rspec'] };
  }
  return null;
}

export async function handleRunTests(command: string) {
  let tc: TestCommand;

  if (command === 'auto') {
    const detected = detectTestCommand();
    if (!detected) {
      return { success: false, exitCode: -1, stdout: '', stderr: '테스트 실행기를 감지할 수 없습니다.' };
    }
    tc = detected;
  } else {
    const parts = command.split(/\s+/);
    assertSafeTestCommand(command);
    tc = { cmd: parts[0], args: parts.slice(1) };
  }

  return runCommand(tc.cmd, tc.args, PROJECT_ROOT);
}
