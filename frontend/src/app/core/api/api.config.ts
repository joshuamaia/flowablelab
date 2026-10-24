/**
 * Base relativa de todas as chamadas da aplicação. O navegador fala apenas com a própria
 * origem; o Nginx (produção) ou o proxy do `ng serve` (desenvolvimento) encaminham ao BFF
 * NestJS, que repassa ao Spring Boot.
 */
export const API_BASE_URL = '/api';
