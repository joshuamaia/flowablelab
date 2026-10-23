import { DiagramState } from '../../core/api/api.models';
import { markersFor, needsCountBadge } from './bpmn-markers';

const state: DiagramState = {
  processInstanceId: 'p1',
  processDefinitionId: 'parallelApproval:1:x',
  ended: false,
  executions: [],
  elements: [
    { elementId: 'start', status: 'COMPLETED', executionCount: 1, failureMessage: null },
    { elementId: 'technicalAnalysis', status: 'COMPLETED', executionCount: 1, failureMessage: null },
    { elementId: 'financialAnalysis', status: 'ACTIVE', executionCount: 1, failureMessage: null },
    { elementId: 'joinGateway', status: 'ACTIVE', executionCount: 1, failureMessage: null },
    { elementId: 'hrRegistration', status: 'FAILED', executionCount: 0, failureMessage: 'Conflito no RH' },
    { elementId: 'review', status: 'ACTIVE', executionCount: 3, failureMessage: null },
  ],
};

describe('markersFor (renderização dos estados BPMN)', () => {
  const markers = markersFor(state);
  const byId = Object.fromEntries(markers.map((m) => [m.elementId, m]));

  it('mapeia cada status para a classe do bpmn-js', () => {
    expect(byId['technicalAnalysis'].marker).toBe('fl-completed');
    expect(byId['financialAnalysis'].marker).toBe('fl-active');
    expect(byId['hrRegistration'].marker).toBe('fl-failed');
  });

  it('não inventa estados para elementos fora do histórico', () => {
    expect(byId['consolidateAnalyses']).toBeUndefined();
    expect(markers).toHaveLength(state.elements.length);
  });

  it('mantém ativa a junção paralela que aguarda ramos', () => {
    expect(byId['joinGateway'].status).toBe('ACTIVE');
  });

  it('exibe contador apenas para elementos executados mais de uma vez', () => {
    expect(needsCountBadge(byId['review'])).toBe(true);
    expect(needsCountBadge(byId['start'])).toBe(false);
  });

  it('retorna vazio sem estado (visualização apenas da definição)', () => {
    expect(markersFor(null)).toEqual([]);
  });
});
