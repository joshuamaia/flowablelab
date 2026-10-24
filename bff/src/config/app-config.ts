import { LogLevel } from '@nestjs/common';

/** Configuração do BFF, lida exclusivamente de variáveis de ambiente e validada na inicialização. */
export class AppConfig {
  constructor(
    /** Porta HTTP do BFF. */
    readonly port: number,
    /** URL base do Spring Boot (ex.: http://backend:8080). */
    readonly backendUrl: URL,
    /** Tempo máximo para o backend começar a responder. */
    readonly proxyTimeoutMs: number,
    readonly logLevels: LogLevel[],
  ) {}
}

const LEVELS: LogLevel[] = ['fatal', 'error', 'warn', 'log', 'debug', 'verbose'];

function integer(env: NodeJS.ProcessEnv, name: string, fallback: number, min: number, max: number): number {
  const raw = env[name];
  if (raw === undefined || raw === '') {
    return fallback;
  }
  const value = Number(raw);
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new Error(`Variável ${name} inválida: "${raw}" (esperado inteiro entre ${min} e ${max}).`);
  }
  return value;
}

/** Lê e valida o ambiente. Lança erro descritivo para impedir a inicialização com configuração inválida. */
export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const rawUrl = env['BACKEND_URL'] || 'http://localhost:8080';
  let backendUrl: URL;
  try {
    backendUrl = new URL(rawUrl);
  } catch {
    throw new Error(`Variável BACKEND_URL inválida: "${rawUrl}".`);
  }
  if (backendUrl.protocol !== 'http:' && backendUrl.protocol !== 'https:') {
    throw new Error(`BACKEND_URL deve usar http ou https: "${rawUrl}".`);
  }
  if (backendUrl.pathname !== '/' || backendUrl.search) {
    throw new Error(`BACKEND_URL deve conter apenas esquema, host e porta: "${rawUrl}".`);
  }

  const level = (env['LOG_LEVEL'] || 'log') as LogLevel;
  if (!LEVELS.includes(level)) {
    throw new Error(`Variável LOG_LEVEL inválida: "${level}" (use ${LEVELS.join(', ')}).`);
  }

  return new AppConfig(
    integer(env, 'PORT', 3000, 1, 65535),
    backendUrl,
    integer(env, 'PROXY_TIMEOUT_MS', 30000, 100, 600000),
    LEVELS.slice(0, LEVELS.indexOf(level) + 1),
  );
}
