import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer';
import { Activity, DiagramState } from '../../core/api/api.models';
import { ACTIVITY_STATUS_LABELS, ACTIVITY_TYPE_LABELS } from '../../core/catalog';
import { DurationPipe } from '../duration.pipe';
import { MARKER_CLASS, markersFor, needsCountBadge } from './bpmn-markers';

interface CanvasApi {
  zoom(level?: number | 'fit-viewport', center?: 'auto' | { x: number; y: number }): number;
  addMarker(element: string, marker: string): void;
  removeMarker(element: string, marker: string): void;
  scrollToElement(element: unknown, padding?: number): void;
  resized(): void;
}

interface OverlaysApi {
  add(element: string, type: string, overlay: { position: object; html: string | HTMLElement }): string;
  remove(filter: { type: string }): void;
}

interface RegistryApi {
  get(id: string): { id: string; type: string; businessObject: { name?: string } } | undefined;
}

interface EventBusApi {
  on(event: string, callback: (e: { element: { id: string; type: string } }) => void): void;
}

export interface SelectedElement {
  id: string;
  name: string;
  type: string;
}

/**
 * Visualizador BPMN baseado em bpmn-js (NavigatedViewer: zoom e arraste).
 * Recebe o XML exato da definição e o estado real de execução calculado pelo backend.
 */
@Component({
  selector: 'fl-bpmn-viewer',
  imports: [MatButtonModule, MatIconModule, MatTooltipModule, DatePipe, DurationPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bpmn-viewer.component.html',
  styleUrl: './bpmn-viewer.component.scss',
})
export class BpmnViewerComponent {
  /** XML BPMN 2.0 com DI. */
  readonly xml = input<string | null | undefined>();
  /** Estado de execução da instância (opcional: sem ele, exibe apenas a definição). */
  readonly state = input<DiagramState | null | undefined>();
  /** Elemento selecionado (sincronizado com a timeline do histórico). */
  readonly selectedId = model<string | null>(null);
  readonly height = input('460px');

  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('canvas');
  private readonly destroyRef = inject(DestroyRef);
  private viewer: NavigatedViewer | null = null;
  private readonly imported = signal(0);
  private appliedMarkers: { id: string; marker: string }[] = [];
  private previousSelection: string | null = null;

  protected readonly error = signal<string | null>(null);
  protected readonly selected = signal<SelectedElement | null>(null);
  protected readonly statusLabels = ACTIVITY_STATUS_LABELS;
  protected readonly typeLabels = ACTIVITY_TYPE_LABELS;

  protected readonly selectedExecutions = computed<Activity[]>(() => {
    const selected = this.selected();
    const state = this.state();
    return selected && state ? state.executions.filter((e) => e.activityId === selected.id) : [];
  });

  protected readonly selectedState = computed(() => {
    const selected = this.selected();
    return selected ? (this.state()?.elements.find((e) => e.elementId === selected.id) ?? null) : null;
  });

  constructor() {
    afterNextRender(() => {
      this.viewer = new NavigatedViewer({ container: this.host().nativeElement });
      this.viewer.get<EventBusApi>('eventBus').on('element.click', (event) => {
        const element = event.element;
        const isRoot = element.type === 'bpmn:Process' || element.type === 'bpmn:Collaboration';
        this.selectedId.set(isRoot ? null : element.id);
      });
      // Dentro de abas/diálogos o contêiner pode nascer com tamanho zero: ajusta quando ganhar tamanho.
      let wasEmpty = true;
      const resize = new ResizeObserver(([entry]) => {
        const empty = entry.contentRect.width === 0 || entry.contentRect.height === 0;
        if (wasEmpty && !empty && this.imported() > 0) {
          this.fit();
        }
        wasEmpty = empty;
      });
      resize.observe(this.host().nativeElement);
      this.destroyRef.onDestroy(() => {
        resize.disconnect();
        this.viewer?.destroy();
      });
      this.importXml(this.xml());
    });

    // Novo XML → reimporta o diagrama.
    effect(() => {
      const xml = this.xml();
      if (this.viewer) {
        this.importXml(xml);
      }
    });

    // Novo estado (após qualquer operação) → reaplica marcadores.
    effect(() => {
      const state = this.state();
      if (this.imported() > 0) {
        this.applyMarkers(state);
      }
    });

    // Seleção vinda da timeline ou de um clique.
    effect(() => {
      const id = this.selectedId();
      if (this.imported() > 0) {
        this.applySelection(id);
      }
    });
  }

  zoomIn(): void {
    this.canvas()?.zoom((this.canvas()?.zoom() ?? 1) * 1.2);
  }

  zoomOut(): void {
    this.canvas()?.zoom((this.canvas()?.zoom() ?? 1) / 1.2);
  }

  fit(): void {
    const container = this.host().nativeElement;
    const canvas = this.canvas();
    if (!canvas || container.clientWidth === 0 || container.clientHeight === 0) {
      return;
    }
    // Descarta o viewbox em cache (calculado enquanto o contêiner estava oculto).
    canvas.resized();
    try {
      canvas.zoom('fit-viewport', 'auto');
    } catch {
      // diagrama ainda sem dimensões válidas; o próximo redimensionamento ajusta novamente
    }
  }

  clearSelection(): void {
    this.selectedId.set(null);
  }

  private canvas(): CanvasApi | null {
    return this.viewer ? this.viewer.get<CanvasApi>('canvas') : null;
  }

  private async importXml(xml: string | null | undefined): Promise<void> {
    if (!this.viewer || !xml) {
      return;
    }
    try {
      await this.viewer.importXML(xml);
    } catch (e) {
      this.error.set('Não foi possível renderizar o diagrama BPMN.');
      console.error(e);
      return;
    }
    this.error.set(null);
    this.appliedMarkers = [];
    this.previousSelection = null;
    this.fit();
    this.imported.update((v) => v + 1);
  }

  private applyMarkers(state: DiagramState | null | undefined): void {
    const canvas = this.canvas();
    if (!canvas || !this.viewer) {
      return;
    }
    const registry = this.viewer.get<RegistryApi>('elementRegistry');
    const overlays = this.viewer.get<OverlaysApi>('overlays');
    this.appliedMarkers.forEach(({ id, marker }) => canvas.removeMarker(id, marker));
    this.appliedMarkers = [];
    overlays.remove({ type: 'fl-count' });
    overlays.remove({ type: 'fl-error' });

    for (const marker of markersFor(state)) {
      if (!registry.get(marker.elementId)) {
        continue; // id inexistente nesta versão do diagrama
      }
      canvas.addMarker(marker.elementId, marker.marker);
      this.appliedMarkers.push({ id: marker.elementId, marker: marker.marker });
      if (needsCountBadge(marker) && !marker.elementId.startsWith('flow')) {
        overlays.add(marker.elementId, 'fl-count', {
          position: { top: -10, right: 10 },
          html: `<span class="fl-count-badge">${marker.executionCount}×</span>`,
        });
      }
      if (marker.marker === MARKER_CLASS.FAILED) {
        overlays.add(marker.elementId, 'fl-error', {
          position: { bottom: -6, left: -6 },
          html: '<span class="fl-error-badge">falha</span>',
        });
      }
    }
    this.updateSelectedDetails(this.selectedId());
  }

  private applySelection(id: string | null): void {
    const canvas = this.canvas();
    if (!canvas || !this.viewer) {
      return;
    }
    if (this.previousSelection) {
      canvas.removeMarker(this.previousSelection, 'fl-selected');
    }
    const element = id ? this.viewer.get<RegistryApi>('elementRegistry').get(id) : undefined;
    if (element) {
      canvas.addMarker(element.id, 'fl-selected');
      if (this.host().nativeElement.clientWidth > 0) {
        canvas.scrollToElement(element, 120);
      }
      this.previousSelection = element.id;
    } else {
      this.previousSelection = null;
    }
    this.updateSelectedDetails(element ? element.id : null);
  }

  private updateSelectedDetails(id: string | null): void {
    const element = id && this.viewer ? this.viewer.get<RegistryApi>('elementRegistry').get(id) : undefined;
    this.selected.set(
      element
        ? {
            id: element.id,
            name: element.businessObject.name || element.id,
            type: element.type.replace('bpmn:', '').replace(/^./, (c) => c.toLowerCase()),
          }
        : null,
    );
  }
}
