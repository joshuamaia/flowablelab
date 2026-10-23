import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { PROCESS_CATALOG } from '../../core/catalog';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { BpmnViewerComponent } from '../../shared/bpmn/bpmn-viewer.component';
import { InstancePickerComponent } from '../../shared/instance-picker.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { instanceResources } from './instance-resources';

/**
 * Tela 5 — Visualizador BPMN. Duas formas de uso:
 *  - ?instance=<id>: diagrama da versão exata usada pela instância + estado real de execução;
 *  - ?definition=<id>: apenas a definição (todos os elementos em cor neutra).
 */
@Component({
  selector: 'fl-viewer-page',
  imports: [
    RouterLink, MatButtonModule, MatIconModule, MatTabsModule, PageHeaderComponent, BpmnViewerComponent,
    InstancePickerComponent, StatusChipComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './viewer.page.html',
  styleUrl: './viewer.page.scss',
})
export class ViewerPage {
  /** Query params vinculados pelo router (withComponentInputBinding). */
  readonly instance = input<string>();
  readonly definition = input<string>();

  private readonly api = inject(WorkflowApi);
  private readonly router = inject(Router);

  protected readonly resources = instanceResources(this.instance);
  protected readonly selectedElement = signal<string | null>(null);

  private readonly definitions = rxResource({ stream: () => this.api.definitions() });
  protected readonly definitionOptions = computed(() =>
    PROCESS_CATALOG.map((meta) => ({ meta, def: this.definitions.value()?.find((d) => d.key === meta.key) }))
      .filter((o) => !!o.def),
  );

  private readonly definitionXml = rxResource({
    params: () => this.definition() || undefined,
    stream: ({ params }) => this.api.definitionXml(params),
  });

  protected readonly mode = computed(() => (this.instance() ? 'instance' : this.definition() ? 'definition' : 'none'));
  protected readonly xml = computed(() =>
    this.mode() === 'instance' ? this.resources.xml.value() : this.definitionXml.value(),
  );
  protected readonly state = computed(() => (this.mode() === 'instance' ? this.resources.diagram.value() : null));
  protected readonly counters = computed(() => {
    const elements = this.state()?.elements.filter((e) => !e.elementId.startsWith('flow')) ?? [];
    return {
      active: elements.filter((e) => e.status === 'ACTIVE').length,
      completed: elements.filter((e) => e.status === 'COMPLETED').length,
      failed: elements.filter((e) => e.status === 'FAILED').length,
    };
  });

  selectInstance(id: string): void {
    this.selectedElement.set(null);
    this.router.navigate([], { queryParams: { instance: id } });
  }

  selectDefinition(id: string): void {
    this.selectedElement.set(null);
    this.router.navigate([], { queryParams: { definition: id } });
  }
}
