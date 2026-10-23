import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TaskAction, WorkTask } from '../core/api/api.models';
import { ACTION_LABELS, VARIABLE_LABELS, processMeta } from '../core/catalog';
import { StatusChipComponent } from './status-chip.component';
import { DurationPipe } from './duration.pipe';

const MONEY_FIELDS = new Set(['amount', 'estimatedAmount']);

/** Card de uma User Task com os dados da solicitação e as ações permitidas pelo backend. */
@Component({
  selector: 'fl-task-card',
  imports: [DatePipe, CurrencyPipe, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule, StatusChipComponent, DurationPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-card.component.html',
  styleUrl: './task-card.component.scss',
})
export class TaskCardComponent {
  readonly task = input.required<WorkTask>();
  readonly busy = input(false);
  /** Perfil atual pode executar a tarefa? (apenas para dica visual; o backend é quem valida) */
  readonly eligible = input(true);
  readonly action = output<TaskAction>();

  protected readonly labels = ACTION_LABELS;
  protected readonly meta = computed(() => processMeta(this.task().processDefinitionKey));
  protected readonly fields = computed(() =>
    Object.entries(VARIABLE_LABELS)
      .filter(([key]) => {
        const value = this.task().variables[key];
        return value !== undefined && value !== null && value !== '';
      })
      .map(([key, label]) => ({ key, label, value: this.task().variables[key], money: MONEY_FIELDS.has(key) })),
  );

  icon(action: TaskAction): string {
    return action === 'APPROVE' ? 'check' : action === 'REJECT' ? 'close' : 'done_all';
  }
}
