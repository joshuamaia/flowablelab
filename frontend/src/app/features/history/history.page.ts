import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { InstancePickerComponent } from '../../shared/instance-picker.component';
import { HistoryTimelineComponent } from '../../shared/history-timeline.component';
import { BpmnViewerComponent } from '../../shared/bpmn/bpmn-viewer.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { instanceResources } from '../processes/instance-resources';

/** Tela 6 — Histórico: timeline real (HistoryService) sincronizada com o diagrama BPMN. */
@Component({
  selector: 'fl-history-page',
  imports: [
    RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent, InstancePickerComponent,
    HistoryTimelineComponent, BpmnViewerComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="fl-page">
      <fl-page-header
        title="Histórico de execução"
        subtitle="Selecione uma atividade na timeline para destacá-la no diagrama BPMN."
      >
        @if (instance()) {
          <a mat-flat-button [routerLink]="['/instances', instance()]"><mat-icon>open_in_new</mat-icon> Detalhes</a>
        }
      </fl-page-header>

      <section class="fl-card picker">
        <fl-instance-picker [selected]="instance() ?? null" (selectedChange)="select($event)" />
      </section>

      @if (instance()) {
        <section class="layout">
          <div class="fl-card timeline">
            <div class="fl-card-header"><h2>Timeline</h2></div>
            <div class="pad">
              <fl-history-timeline
                [activities]="r.activities.value() ?? []"
                [instance]="r.details.value()?.instance ?? null"
                [outcome]="r.outcome()"
                [(selectedId)]="selectedElement"
              />
            </div>
          </div>
          <div class="fl-card diagram">
            <div class="fl-card-header"><h2>Diagrama BPMN</h2></div>
            <div class="pad">
              <fl-bpmn-viewer [xml]="r.xml.value()" [state]="r.diagram.value()" [(selectedId)]="selectedElement" height="600px" />
            </div>
          </div>
        </section>
      } @else {
        <div class="fl-card">
          <fl-empty-state icon="history" title="Escolha uma instância" message="O histórico completo é consultado no Flowable." />
        </div>
      }
    </div>
  `,
  styles: `
    .picker { padding: 16px 20px; margin-bottom: 20px; }
    .layout { display: grid; grid-template-columns: minmax(320px, 1fr) minmax(0, 1.6fr); gap: 20px; align-items: start; }
    @media (max-width: 1100px) { .layout { grid-template-columns: minmax(0, 1fr); } }
    .pad { padding: 8px 16px 16px; }
    .timeline .pad { max-height: 640px; overflow: auto; }
  `,
})
export class HistoryPage {
  readonly instance = input<string>();
  private readonly router = inject(Router);
  protected readonly r = instanceResources(this.instance);
  protected readonly selectedElement = signal<string | null>(null);

  select(id: string): void {
    this.selectedElement.set(null);
    this.router.navigate([], { queryParams: { instance: id } });
  }
}
