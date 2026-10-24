import 'reflect-metadata';
import { ConsoleLogger, Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { AppModule } from './app.module';
import { loadConfig } from './config/app-config';

async function bootstrap(): Promise<void> {
  const config = loadConfig();
  const app = await NestFactory.create<NestExpressApplication>(AppModule.register(config), {
    // Sem body parser: os corpos são repassados em streaming, sem conversão.
    bodyParser: false,
    logger: new ConsoleLogger({ json: true, logLevels: config.logLevels }),
  });
  // Atrás do Nginx: usa X-Forwarded-* para obter IP e protocolo do cliente.
  app.set('trust proxy', true);
  app.disable('x-powered-by');
  app.enableShutdownHooks();

  await app.listen(config.port, '0.0.0.0');
  Logger.log(
    { message: 'BFF iniciado', port: config.port, backendUrl: config.backendUrl.origin, timeoutMs: config.proxyTimeoutMs },
    'Bootstrap',
  );
}

bootstrap().catch((error: Error) => {
  // Configuração inválida ou porta ocupada: falha explícita, sem stack trace extenso.
  console.error(JSON.stringify({ level: 'fatal', context: 'Bootstrap', message: error.message }));
  process.exit(1);
});
