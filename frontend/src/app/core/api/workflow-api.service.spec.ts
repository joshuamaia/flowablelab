import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WorkflowApi } from './workflow-api.service';
import { demoUserInterceptor } from '../http/demo-user.interceptor';
import { DemoUserStore } from '../state/demo-user.store';

describe('WorkflowApi', () => {
  let api: WorkflowApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([demoUserInterceptor])), provideHttpClientTesting()],
    });
    api = TestBed.inject(WorkflowApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia apenas filtros preenchidos ao listar solicitações', () => {
    api.requests({ status: 'APPROVED', type: null, requester: '', page: 2, size: 10 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/requests');
    expect(req.request.params.get('status')).toBe('APPROVED');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.has('type')).toBe(false);
    expect(req.request.params.has('requester')).toBe(false);
    req.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('conclui tarefa com ação, comentário e perfil demonstrativo atual', () => {
    TestBed.inject(DemoUserStore).select('ti');
    let processEnded = false;
    api.completeTask('t-1', 'COMPLETE', 'feito').subscribe((r) => (processEnded = r.processEnded));

    const req = http.expectOne('/api/tasks/t-1/complete');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ action: 'COMPLETE', comment: 'feito' });
    expect(req.request.headers.get('X-Demo-User')).toBe('ti');
    req.flush({ taskId: 't-1', processInstanceId: 'p', processEnded: true, nextTasks: [] });
    expect(processEnded).toBe(true);
  });

  it('obtém o XML BPMN da versão exata como texto', () => {
    let xml = '';
    api.definitionXml('purchaseApproval:2:abc').subscribe((x) => (xml = x));
    const req = http.expectOne('/api/process-definitions/purchaseApproval%3A2%3Aabc/diagram');
    expect(req.request.responseType).toBe('text');
    req.flush('<definitions/>');
    expect(xml).toBe('<definitions/>');
  });

  it('inicia processo enviando o processKey no corpo', () => {
    api.startProcess({ processKey: 'accessRequest', requester: 'Ana', system: 'ERP', accessLevel: 'LEITURA', justification: 'x' }).subscribe();
    const req = http.expectOne('/api/process-instances');
    expect(req.request.body.processKey).toBe('accessRequest');
    req.flush({ request: {}, activeTasks: [] });
  });
});
