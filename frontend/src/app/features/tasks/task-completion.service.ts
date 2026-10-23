import { Injectable, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Observable, catchError, filter, finalize, switchMap, tap } from 'rxjs';
import { TaskAction, TaskCompletion, WorkTask } from '../../core/api/api.models';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { RefreshStore } from '../../core/state/refresh.store';
import { NotifyService } from '../../core/notify.service';
import { ACTION_LABELS } from '../../core/catalog';
import { CompleteTaskDialog, CompleteTaskDialogData, CompleteTaskDialogResult } from './complete-task.dialog';

/**
 * Fluxo de conclusão de tarefa compartilhado pelas telas: coleta comentário/justificativa,
 * chama o backend (que valida responsável e ação) e dispara a atualização global do estado.
 */
@Injectable({ providedIn: 'root' })
export class TaskCompletionService {
  private readonly dialog = inject(MatDialog);
  private readonly api = inject(WorkflowApi);
  private readonly refresh = inject(RefreshStore);
  private readonly notify = inject(NotifyService);

  /** Id da tarefa em processamento (desabilita botões na interface). */
  readonly busyTaskId = signal<string | null>(null);

  run(task: WorkTask, action: TaskAction): void {
    this.dialog
      .open<CompleteTaskDialog, CompleteTaskDialogData, CompleteTaskDialogResult>(CompleteTaskDialog, {
        data: { task, action },
        width: '520px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .pipe(
        filter((result): result is CompleteTaskDialogResult => !!result),
        // O erro já foi notificado em complete(); aqui apenas encerra o fluxo.
        switchMap((result) => this.complete(task, action, result.comment).pipe(catchError(() => EMPTY))),
      )
      .subscribe();
  }

  complete(task: WorkTask, action: TaskAction, comment: string | null): Observable<TaskCompletion> {
    this.busyTaskId.set(task.id);
    return this.api.completeTask(task.id, action, comment || null).pipe(
      tap({
        next: (completion) => {
          const next = completion.processEnded
            ? 'Processo finalizado.'
            : completion.nextTasks.length
              ? `Próxima etapa: ${completion.nextTasks.map((t) => t.name).join(' + ')}.`
              : 'Aguardando etapa automática.';
          this.notify.success(`${ACTION_LABELS[action]}: “${task.name}” concluída. ${next}`);
          this.refresh.bump();
        },
        error: (error) => {
          this.notify.error(error);
          this.refresh.bump(); // estado pode ter mudado (ex.: tarefa já concluída por outro perfil)
        },
      }),
      finalize(() => this.busyTaskId.set(null)),
    );
  }
}
