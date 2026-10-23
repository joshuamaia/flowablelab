import { HttpErrorResponse } from '@angular/common/http';
import { problemMessage } from './problem';

describe('problemMessage', () => {
  it('usa o detail do Problem Details', () => {
    const error = new HttpErrorResponse({ status: 409, error: { title: 'Tarefa já concluída', detail: 'A tarefa x já foi concluída.' } });
    expect(problemMessage(error)).toBe('A tarefa x já foi concluída.');
  });

  it('lista erros de validação por campo', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'inválido', errors: [{ field: 'amount', message: 'deve ser maior que 0' }] },
    });
    expect(problemMessage(error)).toBe('amount: deve ser maior que 0');
  });

  it('interpreta corpo textual (respostas com responseType text)', () => {
    const error = new HttpErrorResponse({ status: 404, error: JSON.stringify({ detail: 'Definição não encontrada' }) });
    expect(problemMessage(error)).toBe('Definição não encontrada');
  });

  it('explica falha de conexão com o backend', () => {
    expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toContain('Não foi possível conectar');
  });
});
