import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'fl-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header>
      <div>
        <h1>{{ title() }}</h1>
        @if (subtitle()) {
          <p>{{ subtitle() }}</p>
        }
      </div>
      <div class="actions"><ng-content /></div>
    </header>
  `,
  styles: `
    header {
      display: flex; align-items: flex-end; justify-content: space-between;
      gap: 16px; flex-wrap: wrap; margin-bottom: 24px;
    }
    h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: -.02em; }
    p { margin: 4px 0 0; color: var(--fl-muted); }
    .actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
  `,
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
