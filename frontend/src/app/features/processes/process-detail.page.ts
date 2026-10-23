import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, JsonPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TaskAction, WorkTask } from '../../core/api/api.models';
import { ACTION_LABELS, processMeta } from '../../core/catalog';
import { problemMessage } from '../../core/http/problem';
import { DemoUserStore } from '../../core/state/demo-user.store';
import { RefreshStore } from '../../core/state/refresh.store';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { BpmnViewerComponent } from '../../shared/bpmn/bpmn-viewer.component';
import { HistoryTimelineComponent } from '../../shared/history-timeline.component';
import { TaskCardComponent } from '../../shared/task-card.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { DurationPipe } from '../../shared/duration.pipe';
import { TaskCompletionService } from '../tasks/task-completion.service';
import { instanceResources } from './instance-resources';

/** Tela 7 — Detalhes do processo: informações, status, variáveis, tarefas, histórico e diagrama. */
@Component({
  selector: 'fl-process-detail-page',
  imports: [
    DatePipe, CurrencyPipe, JsonPipe, RouterLink, MatTabsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatProgressBarModule, PageHeaderComponent, StatusChipComponent, BpmnViewerComponent, HistoryTimelineComponent,
    TaskCardComponent, EmptyStateComponent, DurationPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './process-detail.page.html',
  styleUrl: './process-detail.page.scss',
})
export class ProcessDetailPage {
  /** Parâmetro de rota :id (withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly users = inject(DemoUserStore);
  protected readonly refresh = inject(RefreshStore);
  protected readonly completion = inject(TaskCompletionService);
  protected readonly r = instanceResources(this.id);
  protected readonly selectedElement = signal<string | null>(null);
  protected readonly actionLabels = ACTION_LABELS;
  protected readonly taskColumns = ['name', 'assignee', 'createdAt', 'endedAt', 'duration', 'decision'];
  protected readonly variableColumns = ['name', 'type', 'value', 'lastUpdated'];

  protected readonly meta = computed(() => processMeta(this.r.details.value()?.instance.processDefinitionKey));
  protected readonly error = computed(() => (this.r.details.error() ? problemMessage(this.r.details.error()) : null));

  isEligible(task: WorkTask): boolean {
    const user = this.users.userId();
    return task.assignee ? task.assignee === user : task.candidateGroups.includes(user);
  }

  run(task: WorkTask, action: TaskAction): void {
    this.completion.run(task, action);
  }

  actionLabel(action: TaskAction): string {
    return this.actionLabels[action] ?? action;
  }

  isObject(value: unknown): boolean {
    return value !== null && typeof value === 'object';
  }
}
