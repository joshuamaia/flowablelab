import { DynamicModule, MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { AppConfig, loadConfig } from './config/app-config';
import { HealthController } from './health/health.controller';
import { CorrelationMiddleware } from './observability/correlation.middleware';
import { RequestLoggingMiddleware } from './observability/request-logging.middleware';
import { ProxyController } from './proxy/proxy.controller';
import { ProxyService } from './proxy/proxy.service';

@Module({})
export class AppModule implements NestModule {
  /** A configuração pode ser injetada pelos testes; em produção vem do ambiente. */
  static register(config: AppConfig = loadConfig()): DynamicModule {
    return {
      module: AppModule,
      // HealthController antes do proxy para que /bff/health nunca seja encaminhado.
      controllers: [HealthController, ProxyController],
      providers: [{ provide: AppConfig, useValue: config }, ProxyService],
    };
  }

  configure(consumer: MiddlewareConsumer): void {
    consumer.apply(CorrelationMiddleware, RequestLoggingMiddleware).forRoutes('{*path}');
  }
}
