/**
 * Regressão funcional da V1 através do BFF, contra o ambiente Docker Compose em execução:
 *   navegador (este teste) -> Nginx :4200 -> BFF NestJS -> Spring Boot + Flowable -> PostgreSQL
 *
 * Uso: REGRESSION_BASE_URL=http://localhost:4200 npm run test:regression
 *
 * Cada resposta deve trazer o X-Correlation-Id enviado: o cabeçalho só é devolvido pelo BFF
 * (o Spring Boot não o emite), comprovando que a chamada passou pela nova camada.
 */
import { randomUUID } from 'node:crypto';

const BASE = process.env['REGRESSION_BASE_URL'] ?? 'http://localhost:4200';

interface Task {
  id: string;
  taskDefinitionKey: string;
  candidateGroups: string[];
  status: string;
}

async function call<T = unknown>(method: string, path: string, user = 'colaborador', body?: unknown):
  Promise<{ status: number; body: T; text: string; headers: Headers }> {
  const correlationId = `regressao-${randomUUID()}`;
  const response = await fetch(BASE + path, {
    method,
    headers: {
      'X-Demo-User': user,
      'X-Correlation-Id': correlationId,
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await response.text();
  expect(response.headers.get('x-correlation-id')).toBe(correlationId);
  const isJson = (response.headers.get('content-type') ?? '').includes('json');
  return { status: response.status, body: (isJson && text ? JSON.parse(text) : text) as T, text, headers: response.headers };
}

async function start(payload: Record<string, unknown>): Promise<{ pid: string; tasks: Task[] }> {
  const res = await call<{ request: { processInstanceId: string; status: string }; activeTasks: Task[] }>(
    'POST', '/api/process-instances', 'colaborador', payload);
  expect(res.status).toBe(201);
  expect(res.body.request.status).toBe('IN_PROGRESS');
  return { pid: res.body.request.processInstanceId, tasks: res.body.activeTasks };
}

async function pending(pid: string): Promise<Task[]> {
  const res = await call<Task[]>('GET', `/api/history/processes/${pid}/tasks`);
  return res.body.filter((t) => t.status === 'PENDING');
}

async function complete(task: Task, user: string, action: string, comment?: string) {
  const res = await call<{ processEnded: boolean; nextTasks: Task[] }>(
    'POST', `/api/tasks/${task.id}/complete`, user, { action, comment });
  expect(res.status).toBe(200);
  return res.body;
}

async function requestStatus(pid: string): Promise<string> {
  const res = await call<{ request: { status: string } }>('GET', `/api/process-instances/${pid}`);
  return res.body.request.status;
}

async function elementStates(pid: string): Promise<Record<string, string>> {
  const res = await call<{ elements: { elementId: string; status: string }[] }>('GET', `/api/process-instances/${pid}/activities`);
  return Object.fromEntries(res.body.elements.map((e) => [e.elementId, e.status]));
}

const byKey = (tasks: Task[], key: string) => {
  const task = tasks.find((t) => t.taskDefinitionKey === key);
  if (!task) throw new Error(`tarefa ${key} ausente`);
  return task;
};
const inDays = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);

describe('Regressão V1 via BFF', () => {
  it('Cenário A — compra: iniciar, validar permissões, aprovar e finalizar', async () => {
    const { pid, tasks } = await start({
      processKey: 'purchaseApproval', requester: 'Regressão BFF', description: 'Monitores', amount: 2500,
    });
    const review = byKey(tasks, 'managerReview');

    const forbidden = await call<{ status: number; title: string }>('POST', `/api/tasks/${review.id}/complete`, 'ti', { action: 'APPROVE' });
    expect(forbidden.status).toBe(403);
    expect(forbidden.headers.get('content-type')).toContain('application/problem+json');

    const noJustification = await call('POST', `/api/tasks/${review.id}/complete`, 'gestor', { action: 'REJECT' });
    expect(noJustification.status).toBe(422);

    const result = await complete(review, 'gestor', 'APPROVE', 'ok');
    expect(result.processEnded).toBe(true);
    expect(await requestStatus(pid)).toBe('APPROVED');

    const again = await call('POST', `/api/tasks/${review.id}/complete`, 'gestor', { action: 'APPROVE' });
    expect(again.status).toBe(409);

    const vars = await call<{ name: string; value: unknown }[]>('GET', `/api/process-instances/${pid}/variables`);
    expect(vars.body.find((v) => v.name === 'purchaseOrderNumber')?.value).toMatch(/^PO-/);
  });

  it('Cenário B — paralelo: a junção espera a análise financeira', async () => {
    const { pid, tasks } = await start({
      processKey: 'parallelApproval', requester: 'Regressão BFF', title: 'Data lake', description: 'Analytics',
      estimatedAmount: 9000,
    });
    const opened = await complete(byKey(tasks, 'openRequest'), 'colaborador', 'COMPLETE');
    expect(opened.nextTasks.map((t) => t.taskDefinitionKey).sort()).toEqual(['financialAnalysis', 'technicalAnalysis']);

    const afterTechnical = await complete(byKey(opened.nextTasks, 'technicalAnalysis'), 'ti', 'APPROVE');
    expect(afterTechnical.processEnded).toBe(false);
    expect((await pending(pid)).map((t) => t.taskDefinitionKey)).toEqual(['financialAnalysis']);
    const waiting = await elementStates(pid);
    expect(waiting['joinGateway']).toBe('ACTIVE');
    expect(waiting['consolidateAnalyses']).toBeUndefined();

    const afterFinancial = await complete(byKey(await pending(pid), 'financialAnalysis'), 'gestor', 'APPROVE');
    expect(afterFinancial.processEnded).toBe(true);
    const finished = await elementStates(pid);
    expect(finished['consolidateAnalyses']).toBe('COMPLETED');
    expect(await requestStatus(pid)).toBe('APPROVED');
  });

  it('Cenário C — férias: aprovação e processamento assíncrono até o fim', async () => {
    const start0 = 300 + Math.floor(Math.random() * 300);
    const { pid, tasks } = await start({
      processKey: 'vacationApproval', requester: `Regressão ${randomUUID().slice(0, 6)}`,
      startDate: inDays(start0), endDate: inDays(start0 + 9),
    });
    await complete(byKey(tasks, 'managerReview'), 'gestor', 'APPROVE');

    let status = '';
    for (let i = 0; i < 40 && status !== 'APPROVED'; i++) {
      await new Promise((r) => setTimeout(r, 1000));
      status = await requestStatus(pid);
    }
    expect(status).toBe('APPROVED');
    expect((await elementStates(pid))['hrRegistration']).toBe('COMPLETED');
  });

  it('Cenário D — acesso: gestor aprova e somente a TI conclui', async () => {
    const { pid, tasks } = await start({
      processKey: 'accessRequest', requester: 'Regressão BFF', system: 'ERP', accessLevel: 'LEITURA', justification: 'Teste',
    });
    const afterManager = await complete(byKey(tasks, 'managerReview'), 'gestor', 'APPROVE');
    const itTask = byKey(afterManager.nextTasks, 'itProvisioning');
    expect(itTask.candidateGroups).toEqual(['ti']);

    const wrongTeam = await call('POST', `/api/tasks/${itTask.id}/complete`, 'gestor', { action: 'COMPLETE' });
    expect(wrongTeam.status).toBe(403);

    expect((await complete(itTask, 'ti', 'COMPLETE', 'criado')).processEnded).toBe(true);
    expect(await requestStatus(pid)).toBe('APPROVED');
    const history = await call<{ assignee: string }[]>('GET', `/api/history/processes/${pid}/tasks`);
    expect(history.body.map((t) => t.assignee)).toEqual(['gestor', 'ti']);
  });

  it('Cenário E — visualização BPMN de instâncias ativa e finalizada', async () => {
    const active = await call<{ content: { id: string; processDefinitionId: string }[] }>(
      'GET', '/api/process-instances?finished=false&size=1');
    const finished = await call<{ content: { id: string; processDefinitionId: string }[] }>(
      'GET', '/api/process-instances?finished=true&size=1');

    for (const [instance, ended] of [[active.body.content[0], false], [finished.body.content[0], true]] as const) {
      const state = await call<{ ended: boolean; processDefinitionId: string; elements: { status: string }[] }>(
        'GET', `/api/process-instances/${instance.id}/activities`);
      expect(state.body.ended).toBe(ended);
      expect(state.body.elements.some((e) => e.status === 'ACTIVE')).toBe(!ended);
      expect(state.body.elements.some((e) => e.status === 'COMPLETED')).toBe(true);

      const xml = await call<string>('GET', `/api/process-definitions/${encodeURIComponent(state.body.processDefinitionId)}/diagram`);
      expect(xml.status).toBe(200);
      expect(xml.headers.get('content-type')).toContain('application/xml');
      expect(xml.text.startsWith('<?xml')).toBe(true);
      expect(xml.text).toContain('bpmndi:BPMNDiagram');
    }

    const summary = await call<{ started: number; pendingTasks: number }>('GET', '/api/dashboard/summary');
    expect(summary.body.started).toBeGreaterThan(0);
  });
});
