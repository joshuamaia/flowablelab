import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { WorkflowApi } from '../core/api/workflow-api.service';
import { RefreshStore } from '../core/state/refresh.store';

/** Seletor das instâncias mais recentes (em andamento, finalizadas ou todas). */
@Component({
  selector: 'fl-instance-picker',
  imports: [DatePipe, MatFormFieldModule, MatSelectModule, MatButtonToggleModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="picker">
      <mat-button-toggle-group [value]="finished()" (change)="finished.set($event.value)" hideSingleSelectionIndicator>
        <mat-button-toggle [value]="null">Todas</mat-button-toggle>
        <mat-button-toggle [value]="false">Em andamento</mat-button-toggle>
        <mat-button-toggle [value]="true">Finalizadas</mat-button-toggle>
      </mat-button-toggle-group>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Instância de processo</mat-label>
        <mat-select [value]="selected()" (valueChange)="selectedChange.emit($event)">
          @for (i of options(); track i.id) {
            <mat-option [value]="i.id">
              <strong>{{ i.businessKey }}</strong> · {{ i.processDefinitionName }}
              <small> · {{ i.startTime | date: 'dd/MM HH:mm' }} · {{ i.status === 'RUNNING' ? 'em andamento' : 'finalizada' }}</small>
            </mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>
  `,
  styles: `
    .picker { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
    mat-form-field { min-width: min(460px, 100%); flex: 1; }
    mat-button-toggle-group { background: #fff; }
    small { color: var(--fl-muted); }
  `,
})
export class InstancePickerComponent {
  readonly selected = input<string | null>(null);
  readonly selectedChange = output<string>();

  private readonly api = inject(WorkflowApi);
  private readonly refresh = inject(RefreshStore);
  protected readonly finished = signal<boolean | null>(null);

  private readonly instances = rxResource({
    params: () => ({ finished: this.finished(), size: 50, v: this.refresh.version() }),
    stream: ({ params }) => this.api.instances(params),
  });
  protected readonly options = computed(() => this.instances.value()?.content ?? []);
}
