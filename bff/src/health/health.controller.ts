import { Controller, Get, HttpStatus, Res } from '@nestjs/common';
import { Response } from 'express';
import { AppConfig } from '../config/app-config';

/** Health checks do próprio BFF, fora dos prefixos encaminhados (/api, /swagger-ui, /v3). */
@Controller('bff/health')
export class HealthController {
  constructor(private readonly config: AppConfig) {}

  /** Liveness: o processo NestJS está respondendo (usado pelo Docker Compose). */
  @Get()
  live(): { status: string } {
    return { status: 'UP' };
  }

  /** Readiness: o BFF consegue alcançar o Spring Boot (diagnóstico de falhas de comunicação). */
  @Get('ready')
  async ready(@Res() res: Response): Promise<void> {
    const url = new URL('/actuator/health', this.config.backendUrl).toString();
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(3000) });
      const body = (await response.json()) as { status?: string };
      const up = response.ok && body.status === 'UP';
      res.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
        .json({ status: up ? 'UP' : 'DOWN', backend: { url: this.config.backendUrl.origin, status: body.status ?? response.status } });
    } catch (error) {
      res.status(HttpStatus.SERVICE_UNAVAILABLE).json({
        status: 'DOWN',
        backend: { url: this.config.backendUrl.origin, error: (error as Error).message },
      });
    }
  }
}
