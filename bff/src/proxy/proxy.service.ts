import { Injectable, Logger, OnModuleDestroy } from '@nestjs/common';
import { Request, Response } from 'express';
import * as http from 'node:http';
import * as https from 'node:https';
import { AppConfig } from '../config/app-config';
import { CORRELATION_HEADER, downstreamResponseHeaders, upstreamRequestHeaders } from './headers';
import { sendProblem } from './problem';

/**
 * Proxy HTTP transparente para o Spring Boot.
 *
 * - Corpo da requisição e da resposta trafegam em streaming, sem parsing: JSON, XML BPMN,
 *   Problem Details, 201/204 e respostas vazias chegam ao Angular exatamente como saíram do backend.
 * - Método, path, query string (inclusive parâmetros repetidos) e cabeçalhos fim a fim são preservados.
 * - Nenhuma retentativa automática: início de processo e conclusão de tarefa nunca são reenviados.
 * - Backend indisponível → 502; sem resposta dentro de PROXY_TIMEOUT_MS → 504.
 */
@Injectable()
export class ProxyService implements OnModuleDestroy {
  private readonly logger = new Logger('Proxy');
  private readonly agent: http.Agent;

  constructor(private readonly config: AppConfig) {
    const AgentClass = config.backendUrl.protocol === 'https:' ? https.Agent : http.Agent;
    this.agent = new AgentClass({ keepAlive: true });
  }

  onModuleDestroy(): void {
    this.agent.destroy();
  }

  forward(req: Request, res: Response): void {
    const correlationId = String(req.headers[CORRELATION_HEADER]);
    // originalUrl preserva path e query string exatamente como recebidos.
    const target = new URL(req.originalUrl, this.config.backendUrl);
    const transport = target.protocol === 'https:' ? https : http;
    let timedOut = false;

    const upstream = transport.request(target, {
      method: req.method,
      agent: this.agent,
      headers: upstreamRequestHeaders(req.headers, {
        correlationId,
        clientIp: req.socket.remoteAddress,
        protocol: req.protocol,
      }),
    });

    const timer = setTimeout(() => {
      timedOut = true;
      upstream.destroy(new Error(`sem resposta em ${this.config.proxyTimeoutMs} ms`));
    }, this.config.proxyTimeoutMs);

    upstream.on('response', (upstreamRes) => {
      clearTimeout(timer);
      res.status(upstreamRes.statusCode ?? 502);
      for (const [name, value] of Object.entries(downstreamResponseHeaders(upstreamRes.headers))) {
        if (value !== undefined) {
          res.setHeader(name, value);
        }
      }
      upstreamRes.on('error', (error) => res.destroy(error));
      upstreamRes.pipe(res);
    });

    upstream.on('error', (error: NodeJS.ErrnoException) => {
      clearTimeout(timer);
      const route = `${req.method} ${req.originalUrl.split('?')[0]}`;
      if (res.headersSent) {
        // Resposta já iniciada: só resta interromper a conexão.
        this.logger.warn({ message: 'falha durante o envio da resposta', route, correlationId, error: error.message });
        res.destroy(error);
        return;
      }
      if (timedOut) {
        this.logger.warn({ message: 'timeout do backend', route, correlationId, timeoutMs: this.config.proxyTimeoutMs });
        sendProblem(res, 504, 'backend-timeout', 'Tempo de resposta esgotado',
          'O serviço de processos não respondeu a tempo. A operação pode ter sido concluída; atualize os dados antes de repetir.');
        return;
      }
      this.logger.error({ message: 'backend indisponível', route, correlationId, code: error.code, error: error.message });
      sendProblem(res, 502, 'backend-unavailable', 'Serviço de processos indisponível',
        'Não foi possível comunicar com o backend. Tente novamente em instantes.');
    });

    // Se o navegador desistir antes da resposta, libera a conexão com o backend.
    res.on('close', () => {
      clearTimeout(timer);
      if (!res.writableFinished) {
        upstream.destroy();
      }
    });

    req.pipe(upstream);
  }
}
