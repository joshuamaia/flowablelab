import { Response } from 'express';

const TYPE_BASE = 'https://flowablelab.dev/problems/';

/** Resposta RFC 9457 no mesmo formato usado pelo Spring Boot, para falhas do próprio BFF. */
export function sendProblem(res: Response, status: number, type: string, title: string, detail: string): void {
  res
    .status(status)
    .type('application/problem+json')
    .send(JSON.stringify({ type: TYPE_BASE + type, title, status, detail, instance: res.req.originalUrl.split('?')[0] }));
}
