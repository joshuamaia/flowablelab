import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { DemoUserStore } from '../state/demo-user.store';

/** Anexa o perfil demonstrativo atual às chamadas da API. */
export const demoUserInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }
  const user = inject(DemoUserStore).userId();
  return next(req.clone({ setHeaders: { 'X-Demo-User': user } }));
};
