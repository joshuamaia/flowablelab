import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Activity,
  DashboardSummary,
  DemoUser,
  DiagramState,
  Page,
  ProcessDefinition,
  ProcessInstance,
  ProcessInstanceDetails,
  ProcessTypeCount,
  ProcessVariable,
  RequestStatus,
  RequestType,
  StartProcessRequest,
  StartProcessResponse,
  StatusCount,
  TaskAction,
  TaskCompletion,
  TaskStatus,
  WorkTask,
  WorkflowRequest,
} from './api.models';

export interface PageParams {
  page?: number;
  size?: number;
}

export interface RequestFilters extends PageParams {
  status?: RequestStatus | null;
  type?: RequestType | null;
  requester?: string | null;
}

export interface TaskFilters extends PageParams {
  assignee?: string | null;
  processKey?: string | null;
  status?: TaskStatus;
}

export interface InstanceFilters extends PageParams {
  processKey?: string | null;
  finished?: boolean | null;
  businessKey?: string | null;
}

/** Remove filtros vazios para não enviar parâmetros inúteis. */
function toParams(filters: object): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}

/** Cliente HTTP da API do FlowableLab. Todos os dados vêm do Flowable/backend real. */
@Injectable({ providedIn: 'root' })
export class WorkflowApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  // ----- usuários demonstrativos
  users(): Observable<DemoUser[]> {
    return this.http.get<DemoUser[]>(`${this.base}/users`);
  }

  // ----- definições
  definitions(): Observable<ProcessDefinition[]> {
    return this.http.get<ProcessDefinition[]>(`${this.base}/process-definitions`);
  }

  definitionXml(definitionId: string): Observable<string> {
    return this.http.get(`${this.base}/process-definitions/${encodeURIComponent(definitionId)}/diagram`, {
      responseType: 'text',
    });
  }

  // ----- instâncias
  startProcess(body: StartProcessRequest): Observable<StartProcessResponse> {
    return this.http.post<StartProcessResponse>(`${this.base}/process-instances`, body);
  }

  instances(filters: InstanceFilters = {}): Observable<Page<ProcessInstance>> {
    return this.http.get<Page<ProcessInstance>>(`${this.base}/process-instances`, { params: toParams(filters) });
  }

  instance(id: string): Observable<ProcessInstanceDetails> {
    return this.http.get<ProcessInstanceDetails>(`${this.base}/process-instances/${id}`);
  }

  diagramState(id: string): Observable<DiagramState> {
    return this.http.get<DiagramState>(`${this.base}/process-instances/${id}/activities`);
  }

  variables(id: string): Observable<ProcessVariable[]> {
    return this.http.get<ProcessVariable[]>(`${this.base}/process-instances/${id}/variables`);
  }

  // ----- solicitações
  requests(filters: RequestFilters = {}): Observable<Page<WorkflowRequest>> {
    return this.http.get<Page<WorkflowRequest>>(`${this.base}/requests`, { params: toParams(filters) });
  }

  // ----- tarefas
  tasks(filters: TaskFilters = {}): Observable<Page<WorkTask>> {
    return this.http.get<Page<WorkTask>>(`${this.base}/tasks`, { params: toParams(filters) });
  }

  completeTask(taskId: string, action: TaskAction, comment?: string | null): Observable<TaskCompletion> {
    return this.http.post<TaskCompletion>(`${this.base}/tasks/${taskId}/complete`, { action, comment });
  }

  // ----- histórico
  historyActivities(id: string): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.base}/history/processes/${id}/activities`);
  }

  historyTasks(id: string): Observable<WorkTask[]> {
    return this.http.get<WorkTask[]>(`${this.base}/history/processes/${id}/tasks`);
  }

  // ----- dashboard
  summary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.base}/dashboard/summary`);
  }

  statusDistribution(): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>(`${this.base}/dashboard/process-status`);
  }

  typeDistribution(): Observable<ProcessTypeCount[]> {
    return this.http.get<ProcessTypeCount[]>(`${this.base}/dashboard/process-types`);
  }
}
