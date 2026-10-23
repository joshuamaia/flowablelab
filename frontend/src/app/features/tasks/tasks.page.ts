import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TaskAction, TaskStatus, WorkTask } from '../../core/api/api.models';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { DemoUserStore } from '../../core/state/demo-user.store';
import { RefreshStore } from '../../core/state/refresh.store';
import { PROCESS_CATALOG } from '../../core/catalog';
import { problemMessage } from '../../core/http/problem';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { TaskCardComponent } from '../../shared/task-card.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { TaskCompletionService } from './task-completion.service';

type Scope = 'mine' | 'all' | 'done';

@Component({
  selector: 'fl-tasks-page',
  imports: [
    MatButtonToggleModule, MatFormFieldModule, MatSelectModule, MatIconModule, MatButtonModule, MatPaginatorModule,
    MatProgressBarModule, PageHeaderComponent, TaskCardComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tasks.page.html',
  styleUrl: './tasks.page.scss',
})
export class TasksPage {
  private readonly api = inject(WorkflowApi);
  private readonly refresh = inject(RefreshStore);
  protected readonly users = inject(DemoUserStore);
  protected readonly completion = inject(TaskCompletionService);

  protected readonly processes = PROCESS_CATALOG;
  protected readonly scope = signal<Scope>('mine');
  protected readonly processKey = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly size = signal(12);

  /** Tarefas reais do Flowable; recarregadas ao trocar perfil/filtros ou após qualquer operação. */
  protected readonly tasks = rxResource({
    params: () => ({
      assignee: this.scope() === 'all' ? null : this.users.userId(),
      status: (this.scope() === 'done' ? 'COMPLETED' : 'PENDING') as TaskStatus,
      processKey: this.processKey(),
      page: this.page(),
      size: this.size(),
      v: this.refresh.version(),
    }),
    stream: ({ params }) => this.api.tasks(params),
  });

  protected readonly items = computed(() => this.tasks.value()?.content ?? []);
  protected readonly total = computed(() => this.tasks.value()?.totalElements ?? 0);
  protected readonly error = computed(() => (this.tasks.error() ? problemMessage(this.tasks.error()) : null));

  setScope(scope: Scope): void {
    this.scope.set(scope);
    this.page.set(0);
  }

  setProcess(key: string | null): void {
    this.processKey.set(key);
    this.page.set(0);
  }

  onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
  }

  isEligible(task: WorkTask): boolean {
    const user = this.users.userId();
    return task.assignee ? task.assignee === user : task.candidateGroups.includes(user);
  }

  run(task: WorkTask, action: TaskAction): void {
    this.completion.run(task, action);
  }
}
