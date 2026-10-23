import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'fl-empty-state',
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty">
      <mat-icon>{{ icon() }}</mat-icon>
      <strong>{{ title() }}</strong>
      @if (message()) {
        <span>{{ message() }}</span>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .empty {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 40px 16px; text-align: center; color: var(--fl-muted);
    }
    mat-icon { font-size: 40px; width: 40px; height: 40px; color: #c5c3d8; }
    strong { color: var(--fl-text); }
  `,
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input<string>();
}
