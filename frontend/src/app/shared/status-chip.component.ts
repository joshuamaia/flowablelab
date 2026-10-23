import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

type ChipStatus =
  | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'FAILED'
  | 'ACTIVE' | 'COMPLETED' | 'PENDING' | 'CANCELED' | 'RUNNING';

const CONFIG: Record<ChipStatus, { label: string; icon: string; tone: string }> = {
  IN_PROGRESS: { label: 'Em andamento', icon: 'autorenew', tone: 'active' },
  RUNNING: { label: 'Em andamento', icon: 'autorenew', tone: 'active' },
  ACTIVE: { label: 'Ativa', icon: 'play_circle', tone: 'active' },
  PENDING: { label: 'Pendente', icon: 'hourglass_top', tone: 'warning' },
  APPROVED: { label: 'Aprovada', icon: 'check_circle', tone: 'success' },
  COMPLETED: { label: 'Concluída', icon: 'task_alt', tone: 'success' },
  REJECTED: { label: 'Rejeitada', icon: 'cancel', tone: 'danger' },
  FAILED: { label: 'Falha', icon: 'error', tone: 'danger' },
  CANCELED: { label: 'Cancelada', icon: 'block', tone: 'neutral' },
};

@Component({
  selector: 'fl-status-chip',
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="chip" [class]="'chip ' + config().tone">
      <mat-icon>{{ config().icon }}</mat-icon>{{ label() ?? config().label }}
    </span>
  `,
  styles: `
    .chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 10px 3px 6px; border-radius: 999px;
      font-size: 12px; font-weight: 600; white-space: nowrap;
    }
    mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .active { background: var(--fl-active-soft); color: var(--fl-active); }
    .warning { background: var(--fl-warning-soft); color: var(--fl-warning); }
    .success { background: var(--fl-completed-soft); color: var(--fl-completed); }
    .danger { background: var(--fl-failed-soft); color: var(--fl-failed); }
    .neutral { background: #f1f1f6; color: var(--fl-muted); }
  `,
})
export class StatusChipComponent {
  readonly status = input.required<string>();
  readonly label = input<string>();
  protected readonly config = computed(
    () => CONFIG[this.status() as ChipStatus] ?? { label: this.status(), icon: 'info', tone: 'neutral' },
  );
}
