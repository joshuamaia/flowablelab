import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'fl-kpi-card',
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="kpi fl-card">
      <div class="icon" [style.background]="soft()" [style.color]="color()">
        <mat-icon>{{ icon() }}</mat-icon>
      </div>
      <div class="body">
        <span class="label">{{ label() }}</span>
        <strong class="value">{{ value() ?? '—' }}</strong>
        @if (hint()) {
          <span class="hint">{{ hint() }}</span>
        }
      </div>
    </div>
  `,
  styles: `
    .kpi { display: flex; gap: 14px; align-items: center; padding: 18px; height: 100%; box-sizing: border-box; }
    .icon {
      width: 48px; height: 48px; border-radius: 14px; flex-shrink: 0;
      display: grid; place-items: center;
    }
    .body { display: flex; flex-direction: column; min-width: 0; }
    .label { color: var(--fl-muted); font-size: 13px; }
    .value { font-size: 28px; line-height: 1.15; font-weight: 700; letter-spacing: -.02em; }
    .hint { color: var(--fl-muted); font-size: 12px; }
  `,
})
export class KpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input<number | string | null>();
  readonly icon = input('insights');
  readonly color = input('#6d4aff');
  readonly soft = input('#efeaff');
  readonly hint = input<string>();
}
