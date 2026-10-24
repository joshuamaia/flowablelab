import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import * as http from 'node:http';
import { AddressInfo } from 'node:net';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { AppConfig } from '../src/config/app-config';

/** Requisição recebida pelo backend de teste. */
interface Received {
  method: string;
  url: string;
  headers: http.IncomingHttpHeaders;
  body: string;
}

const BPMN_XML = '<?xml version="1.0" encoding="UTF-8"?><definitions><process id="purchaseApproval"/></definitions>';

/**
 * Backend HTTP real que imita os comportamentos do Spring Boot relevantes para o proxy,
 * sem depender do Flowable. Registra tudo o que recebe.
 */
function startFakeBackend(): Promise<{ server: http.Server; url: string; received: Received[] }> {
  const received: Received[] = [];
  const server = http.createServer((req, res) => {
    const chunks: Buffer[] = [];
    req.on('data', (c: Buffer) => chunks.push(c));
    req.on('end', () => {
      const entry = { method: req.method!, url: req.url!, headers: req.headers, body: Buffer.concat(chunks).toString() };
      received.push(entry);
      const path = req.url!.split('?')[0];

      if (path === '/api/reset') {
        req.socket.destroy(); // falha de conexão sem resposta
        return;
      }
      if (path === '/api/slow') {
        setTimeout(() => res.end('{}'), 1500).unref();
        return;
      }
      if (path === '/api/process-definitions/purchaseApproval%3A1%3Aabc/diagram') {
        res.writeHead(200, { 'Content-Type': 'application/xml;charset=UTF-8' }).end(BPMN_XML);
        return;
      }
      if (path === '/api/empty') {
        res.writeHead(204).end();
        return;
      }
      if (path === '/api/process-instances' && req.method === 'POST') {
        res.writeHead(201, { 'Content-Type': 'application/json', Location: `http://${req.headers.host}/api/process-instances/pi-1` })
          .end(JSON.stringify({ request: { number: 'REQ-1' } }));
        return;
      }
      const status = /^\/api\/status\/(\d{3})$/.exec(path);
      if (status) {
        const code = Number(status[1]);
        res.writeHead(code, { 'Content-Type': 'application/problem+json' })
          .end(JSON.stringify({ title: `erro ${code}`, status: code, detail: `detalhe do backend ${code}` }));
        return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json', 'X-Backend': 'fake' }).end(JSON.stringify(entry));
    });
  });
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () =>
    resolve({ server, url: `http://127.0.0.1:${(server.address() as AddressInfo).port}`, received })));
}

async function createBff(backendUrl: string, timeoutMs = 400): Promise<INestApplication> {
  const config = new AppConfig(0, new URL(backendUrl), timeoutMs, ['error']);
  const moduleRef = await Test.createTestingModule({ imports: [AppModule.register(config)] }).compile();
  const app = moduleRef.createNestApplication({ bodyParser: false, logger: ['error'] });
  await app.init();
  return app;
}

describe('Proxy BFF → Spring Boot', () => {
  let backend: Awaited<ReturnType<typeof startFakeBackend>>;
  let app: INestApplication;

  beforeAll(async () => {
    backend = await startFakeBackend();
    app = await createBff(backend.url);
  });

  afterAll(async () => {
    await app.close();
    backend.server.closeAllConnections();
    await new Promise((r) => backend.server.close(r));
  });

  beforeEach(() => (backend.received.length = 0));

  const last = () => backend.received[backend.received.length - 1];

  it('encaminha GET preservando path, parâmetros de rota e query repetida', async () => {
    const res = await request(app.getHttpServer())
      .get('/api/tasks/abc-123?assignee=gestor&status=PENDING&tag=a&tag=b&q=a%20b')
      .expect(200);

    expect(last().method).toBe('GET');
    expect(last().url).toBe('/api/tasks/abc-123?assignee=gestor&status=PENDING&tag=a&tag=b&q=a%20b');
    expect(res.headers['x-backend']).toBe('fake');
    expect(res.body.url).toBe(last().url);
  });

  it('encaminha POST com corpo, Content-Type, X-Demo-User e Authorization intactos', async () => {
    const body = '{"action":"REJECT","comment":"Fora da política — ç"}';
    await request(app.getHttpServer())
      .post('/api/tasks/t1/complete')
      .set('Content-Type', 'application/json')
      .set('X-Demo-User', 'gestor')
      .set('Authorization', 'Bearer token-123')
      .send(body)
      .expect(200);

    expect(last().method).toBe('POST');
    expect(last().body).toBe(body);
    expect(last().headers['content-type']).toBe('application/json');
    expect(last().headers['x-demo-user']).toBe('gestor');
    expect(last().headers['authorization']).toBe('Bearer token-123');
  });

  it.each(['put', 'patch', 'delete'] as const)('encaminha %s', async (method) => {
    await request(app.getHttpServer())[method]('/api/resource/9').send('{"a":1}').set('Content-Type', 'application/json')
      .expect(200);
    expect(last().method).toBe(method.toUpperCase());
    expect(last().url).toBe('/api/resource/9');
    expect(last().body).toBe('{"a":1}');
  });

  it('preserva status 201, corpo e Location gerado com o Host original', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/process-instances')
      .set('Host', 'localhost:4200')
      .set('Content-Type', 'application/json')
      .send('{"processKey":"purchaseApproval"}')
      .expect(201);
    expect(res.body).toEqual({ request: { number: 'REQ-1' } });
    expect(res.headers['location']).toBe('http://localhost:4200/api/process-instances/pi-1');
  });

  it('entrega o XML BPMN byte a byte, sem conversão para JSON', async () => {
    const res = await request(app.getHttpServer())
      .get('/api/process-definitions/purchaseApproval%3A1%3Aabc/diagram')
      .buffer(true)
      .parse((r, cb) => { let data = ''; r.on('data', (c: string) => (data += c)); r.on('end', () => cb(null, data)); })
      .expect(200);
    expect(res.headers['content-type']).toBe('application/xml;charset=UTF-8');
    expect(res.body).toBe(BPMN_XML);
  });

  it('preserva respostas vazias 204', async () => {
    const res = await request(app.getHttpServer()).delete('/api/empty').expect(204);
    expect(res.text).toBe('');
  });

  it.each([400, 401, 403, 404, 409, 422, 500])('repassa erro %i do backend com o Problem Details original', async (code) => {
    const res = await request(app.getHttpServer()).get(`/api/status/${code}`).expect(code);
    expect(res.headers['content-type']).toContain('application/problem+json');
    expect(res.body).toEqual({ title: `erro ${code}`, status: code, detail: `detalhe do backend ${code}` });
  });

  it('propaga o X-Correlation-Id recebido e o devolve ao cliente', async () => {
    const res = await request(app.getHttpServer()).get('/api/users').set('X-Correlation-Id', 'teste-123').expect(200);
    expect(last().headers['x-correlation-id']).toBe('teste-123');
    expect(res.headers['x-correlation-id']).toBe('teste-123');
  });

  it('gera X-Correlation-Id quando ausente ou inválido', async () => {
    const res = await request(app.getHttpServer()).get('/api/users').set('X-Correlation-Id', 'inválido com espaço').expect(200);
    expect(res.headers['x-correlation-id']).toMatch(/^[0-9a-f-]{36}$/);
    expect(last().headers['x-correlation-id']).toBe(res.headers['x-correlation-id']);
  });

  it('encaminha também o Swagger e o OpenAPI do backend', async () => {
    await request(app.getHttpServer()).get('/swagger-ui/index.html').expect(200);
    expect(last().url).toBe('/swagger-ui/index.html');
    await request(app.getHttpServer()).get('/v3/api-docs').expect(200);
    expect(last().url).toBe('/v3/api-docs');
  });

  it('não encaminha rotas fora dos prefixos nem o health do BFF', async () => {
    await request(app.getHttpServer()).get('/bff/health').expect(200, { status: 'UP' });
    await request(app.getHttpServer()).get('/actuator/health').expect(404);
    expect(backend.received).toHaveLength(0);
  });

  it('responde 504 em Problem Details quando o backend não responde a tempo', async () => {
    const res = await request(app.getHttpServer()).get('/api/slow').expect(504);
    expect(res.headers['content-type']).toContain('application/problem+json');
    expect(res.body).toMatchObject({ status: 504, title: 'Tempo de resposta esgotado', instance: '/api/slow' });
  });

  it('não repete operações de escrita quando a conexão falha (sem retries)', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/reset')
      .set('Content-Type', 'application/json')
      .send('{"processKey":"purchaseApproval"}')
      .expect(502);
    expect(res.body.title).toBe('Serviço de processos indisponível');
    await new Promise((r) => setTimeout(r, 200));
    expect(backend.received.filter((r) => r.url === '/api/reset')).toHaveLength(1);
  });
});

describe('Proxy com backend indisponível', () => {
  it('responde 502 em Problem Details e o readiness fica DOWN', async () => {
    // Porta reservada e liberada: nada escuta nela.
    const probe = http.createServer();
    await new Promise<void>((r) => probe.listen(0, '127.0.0.1', () => r()));
    const port = (probe.address() as AddressInfo).port;
    await new Promise((r) => probe.close(r));

    const app = await createBff(`http://127.0.0.1:${port}`);
    try {
      const res = await request(app.getHttpServer()).get('/api/dashboard/summary').expect(502);
      expect(res.body).toMatchObject({ status: 502, type: 'https://flowablelab.dev/problems/backend-unavailable' });
      expect(JSON.stringify(res.body)).not.toContain('ECONNREFUSED');

      const ready = await request(app.getHttpServer()).get('/bff/health/ready').expect(503);
      expect(ready.body.status).toBe('DOWN');
      await request(app.getHttpServer()).get('/bff/health').expect(200);
    } finally {
      await app.close();
    }
  });
});
