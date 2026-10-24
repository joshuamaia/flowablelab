import { Injectable, Logger, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';
import { CORRELATION_HEADER } from '../proxy/headers';

/**
 * Uma linha de log por requisição: método, rota (sem query string), status, duração e
 * correlação. Corpos e cabeçalhos não são registrados para não expor dados sensíveis.
 */
@Injectable()
export class RequestLoggingMiddleware implements NestMiddleware {
  private readonly logger = new Logger('HTTP');

  use(req: Request, res: Response, next: NextFunction): void {
    const start = process.hrtime.bigint();
    res.on('close', () => {
      const entry = {
        method: req.method,
        path: req.originalUrl.split('?')[0],
        status: res.statusCode,
        durationMs: Number((process.hrtime.bigint() - start) / 1_000_000n),
        correlationId: req.headers[CORRELATION_HEADER],
        aborted: !res.writableFinished || undefined,
      };
      if (res.statusCode >= 500 || entry.aborted) {
        this.logger.warn(entry);
      } else {
        this.logger.log(entry);
      }
    });
    next();
  }
}
