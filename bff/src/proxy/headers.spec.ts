import { downstreamResponseHeaders, upstreamRequestHeaders } from './headers';

describe('headers', () => {
  const ctx = { correlationId: 'corr-1', clientIp: '172.18.0.5', protocol: 'http' };

  it('encaminha cabeçalhos fim a fim e remove hop-by-hop', () => {
    const headers = upstreamRequestHeaders({
      host: 'localhost',
      'content-type': 'application/json',
      authorization: 'Bearer abc',
      'x-demo-user': 'gestor',
      connection: 'keep-alive, x-secret-hop',
      'x-secret-hop': '1',
      'transfer-encoding': 'chunked',
      'keep-alive': 'timeout=5',
    }, ctx);

    expect(headers).toMatchObject({
      host: 'localhost',
      'content-type': 'application/json',
      authorization: 'Bearer abc',
      'x-demo-user': 'gestor',
      'x-correlation-id': 'corr-1',
      'x-forwarded-host': 'localhost',
      'x-forwarded-proto': 'http',
    });
    expect(headers).not.toHaveProperty('connection');
    expect(headers).not.toHaveProperty('x-secret-hop');
    expect(headers).not.toHaveProperty('transfer-encoding');
    expect(headers).not.toHaveProperty('keep-alive');
  });

  it('acrescenta o IP do cliente à cadeia X-Forwarded-For existente', () => {
    const headers = upstreamRequestHeaders({ 'x-forwarded-for': '10.0.0.1', 'x-forwarded-proto': 'https' }, ctx);
    expect(headers['x-forwarded-for']).toBe('10.0.0.1, 172.18.0.5');
    expect(headers['x-forwarded-proto']).toBe('https');
  });

  it('devolve cabeçalhos de resposta relevantes sem hop-by-hop', () => {
    const headers = downstreamResponseHeaders({
      'content-type': 'application/xml',
      location: 'http://localhost/api/process-instances/1',
      'set-cookie': ['a=1', 'b=2'],
      'transfer-encoding': 'chunked',
      connection: 'close',
    });
    expect(headers).toEqual({
      'content-type': 'application/xml',
      location: 'http://localhost/api/process-instances/1',
      'set-cookie': ['a=1', 'b=2'],
    });
  });
});
