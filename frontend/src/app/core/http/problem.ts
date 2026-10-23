import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../api/api.models';

/** Extrai uma mensagem amigável de um erro HTTP (Problem Details do backend). */
export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Não foi possível conectar ao backend. Verifique se os contêineres estão em execução.';
    }
    const body = parseBody(error.error);
    if (body?.errors?.length) {
      return body.errors.map((e) => `${e.field}: ${e.message}`).join(' · ');
    }
    if (body?.detail) {
      return body.detail;
    }
    if (body?.title) {
      return body.title;
    }
    return `Erro ${error.status} ao comunicar com o servidor.`;
  }
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

function parseBody(raw: unknown): ProblemDetail | null {
  if (raw && typeof raw === 'object') {
    return raw as ProblemDetail;
  }
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as ProblemDetail;
    } catch {
      return null;
    }
  }
  return null;
}
