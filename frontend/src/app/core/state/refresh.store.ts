import { Injectable, signal } from '@angular/core';

/**
 * Sinal global de atualização. Após qualquer operação (início de processo, conclusão de
 * tarefa) ou clique em "Atualizar", a versão é incrementada e todos os recursos que a
 * leem — solicitações, tarefas, histórico, diagrama e indicadores — são recarregados.
 * Não há polling contínuo.
 */
@Injectable({ providedIn: 'root' })
export class RefreshStore {
  private readonly counter = signal(0);
  private readonly lastAt = signal<Date>(new Date());

  readonly version = this.counter.asReadonly();
  readonly lastRefresh = this.lastAt.asReadonly();

  bump(): void {
    this.counter.update((v) => v + 1);
    this.lastAt.set(new Date());
  }
}
