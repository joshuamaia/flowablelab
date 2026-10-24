import { Injectable, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';
import { randomUUID } from 'node:crypto';
import { CORRELATION_HEADER } from '../proxy/headers';

const VALID_ID = /^[A-Za-z0-9._:-]{1,128}$/;

/**
 * Garante um X-Correlation-Id em cada requisição: reutiliza o recebido (se válido) ou gera um
 * novo. O mesmo valor é enviado ao Spring Boot e devolvido ao navegador.
 */
@Injectable()
export class CorrelationMiddleware implements NestMiddleware {
  use(req: Request, res: Response, next: NextFunction): void {
    const received = req.headers[CORRELATION_HEADER];
    const id = typeof received === 'string' && VALID_ID.test(received) ? received : randomUUID();
    req.headers[CORRELATION_HEADER] = id;
    res.setHeader(CORRELATION_HEADER, id);
    next();
  }
}
