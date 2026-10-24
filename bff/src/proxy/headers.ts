import { IncomingHttpHeaders, OutgoingHttpHeaders } from 'node:http';

/** Cabeçalhos hop-by-hop (RFC 9110 §7.6.1): válidos apenas para uma conexão, nunca encaminhados. */
const HOP_BY_HOP = new Set([
  'connection',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'proxy-connection',
  'te',
  'trailer',
  'transfer-encoding',
  'upgrade',
]);

export const CORRELATION_HEADER = 'x-correlation-id';

function connectionTokens(headers: IncomingHttpHeaders): Set<string> {
  const value = headers['connection'];
  const raw = Array.isArray(value) ? value.join(',') : (value ?? '');
  return new Set(raw.split(',').map((t) => t.trim().toLowerCase()).filter(Boolean));
}

function copyEndToEnd(headers: IncomingHttpHeaders): OutgoingHttpHeaders {
  const extra = connectionTokens(headers);
  const result: OutgoingHttpHeaders = {};
  for (const [name, value] of Object.entries(headers)) {
    const lower = name.toLowerCase();
    if (value !== undefined && !HOP_BY_HOP.has(lower) && !extra.has(lower)) {
      result[lower] = value;
    }
  }
  return result;
}

export interface ForwardContext {
  correlationId: string;
  clientIp: string | undefined;
  protocol: string;
}

/**
 * Cabeçalhos enviados ao Spring Boot: todos os cabeçalhos fim a fim do navegador
 * (Content-Type, Accept, Authorization, X-Demo-User, Host...) mais os de rastreabilidade.
 * O Host original é mantido para que URLs geradas pelo backend (ex.: Location) não mudem.
 */
export function upstreamRequestHeaders(incoming: IncomingHttpHeaders, ctx: ForwardContext): OutgoingHttpHeaders {
  const headers = copyEndToEnd(incoming);
  headers[CORRELATION_HEADER] = ctx.correlationId;

  const priorFor = incoming['x-forwarded-for'];
  const chain = [Array.isArray(priorFor) ? priorFor.join(', ') : priorFor, ctx.clientIp].filter(Boolean).join(', ');
  if (chain) {
    headers['x-forwarded-for'] = chain;
  }
  headers['x-forwarded-proto'] = incoming['x-forwarded-proto'] ?? ctx.protocol;
  if (incoming['host'] && !incoming['x-forwarded-host']) {
    headers['x-forwarded-host'] = incoming['host'];
  }
  return headers;
}

/** Cabeçalhos devolvidos ao navegador: tudo o que o backend respondeu, exceto hop-by-hop. */
export function downstreamResponseHeaders(upstream: IncomingHttpHeaders): OutgoingHttpHeaders {
  return copyEndToEnd(upstream);
}
