import { ActivityStatus, DiagramState } from '../../core/api/api.models';

export const MARKER_CLASS: Record<ActivityStatus, string> = {
  ACTIVE: 'fl-active',
  COMPLETED: 'fl-completed',
  FAILED: 'fl-failed',
};

export interface ElementMarker {
  elementId: string;
  marker: string;
  status: ActivityStatus;
  executionCount: number;
  failureMessage: string | null;
}

/**
 * Converte o estado calculado pelo backend (histórico real do Flowable) em marcadores do
 * bpmn-js. Somente elementos presentes no histórico recebem marcação; elementos não
 * executados permanecem neutros. Nenhum estado é inferido no frontend.
 */
export function markersFor(state: DiagramState | null | undefined): ElementMarker[] {
  if (!state) {
    return [];
  }
  return state.elements.map((element) => ({
    elementId: element.elementId,
    marker: MARKER_CLASS[element.status],
    status: element.status,
    executionCount: element.executionCount,
    failureMessage: element.failureMessage,
  }));
}

/** Elementos executados mais de uma vez (loops) recebem um contador sobreposto. */
export function needsCountBadge(marker: ElementMarker): boolean {
  return marker.executionCount > 1;
}
