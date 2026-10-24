import { All, Controller, Req, Res } from '@nestjs/common';
import { Request, Response } from 'express';
import { ProxyService } from './proxy.service';

/**
 * Rotas encaminhadas ao Spring Boot sem alteração: a API REST e a documentação OpenAPI.
 * Um único handler evita duplicar manualmente cada endpoint do backend; agregações ou
 * adaptações futuras podem ser declaradas em controllers próprios antes deste.
 */
@Controller()
export class ProxyController {
  constructor(private readonly proxy: ProxyService) {}

  @All(['api', 'api/*path', 'swagger-ui.html', 'swagger-ui/*path', 'v3/api-docs', 'v3/api-docs/*path'])
  forward(@Req() req: Request, @Res() res: Response): void {
    this.proxy.forward(req, res);
  }
}
