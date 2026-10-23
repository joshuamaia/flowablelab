import { Signal, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { RefreshStore } from '../../core/state/refresh.store';
import { STATUS_LABELS } from '../../core/catalog';

/**
 * Recursos reativos de uma instância de processo. Todos dependem do id e da versão global
 * de atualização: após qualquer operação, detalhes, diagrama, histórico e variáveis são
 * consultados novamente no backend. Deve ser chamado em contexto de injeção.
 */
export function instanceResources(instanceId: Signal<string | null | undefined>) {
  const api = inject(WorkflowApi);
  const refresh = inject(RefreshStore);
  const key = () => {
    const id = instanceId();
    return id ? { id, v: refresh.version() } : undefined;
  };

  const details = rxResource({ params: key, stream: ({ params }) => api.instance(params.id) });
  const diagram = rxResource({ params: key, stream: ({ params }) => api.diagramState(params.id) });
  const activities = rxResource({ params: key, stream: ({ params }) => api.historyActivities(params.id) });
  const tasks = rxResource({ params: key, stream: ({ params }) => api.historyTasks(params.id) });
  const variables = rxResource({ params: key, stream: ({ params }) => api.variables(params.id) });

  // O XML depende apenas da versão exata da definição usada pela instância (não muda após operações).
  const definitionId = computed(() => (diagram.hasValue() ? diagram.value().processDefinitionId : undefined));
  const xml = rxResource({ params: definitionId, stream: ({ params }) => api.definitionXml(params) });

  const outcome = computed(() => {
    const d = details.value();
    if (!d) {
      return null;
    }
    if (d.request) {
      return STATUS_LABELS[d.request.status];
    }
    return d.instance.status === 'COMPLETED' ? 'Concluído' : null;
  });

  return { details, diagram, activities, tasks, variables, xml, outcome };
}
