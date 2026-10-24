import { loadConfig } from './app-config';

describe('loadConfig', () => {
  it('usa padrões quando o ambiente está vazio', () => {
    const config = loadConfig({});
    expect(config.port).toBe(3000);
    expect(config.backendUrl.origin).toBe('http://localhost:8080');
    expect(config.proxyTimeoutMs).toBe(30000);
    expect(config.logLevels).toEqual(['fatal', 'error', 'warn', 'log']);
  });

  it('lê BACKEND_URL, PORT e PROXY_TIMEOUT_MS', () => {
    const config = loadConfig({ BACKEND_URL: 'http://backend:8080', PORT: '3100', PROXY_TIMEOUT_MS: '5000' });
    expect(config.backendUrl.host).toBe('backend:8080');
    expect(config.port).toBe(3100);
    expect(config.proxyTimeoutMs).toBe(5000);
  });

  it.each([
    [{ BACKEND_URL: 'não é url' }, 'BACKEND_URL inválida'],
    [{ BACKEND_URL: 'ftp://backend' }, 'http ou https'],
    [{ BACKEND_URL: 'http://backend:8080/api' }, 'apenas esquema'],
    [{ PORT: 'abc' }, 'PORT inválida'],
    [{ PROXY_TIMEOUT_MS: '10' }, 'PROXY_TIMEOUT_MS inválida'],
    [{ LOG_LEVEL: 'trace' }, 'LOG_LEVEL inválida'],
  ])('rejeita configuração inválida %p', (env, message) => {
    expect(() => loadConfig(env)).toThrow(message);
  });
});
