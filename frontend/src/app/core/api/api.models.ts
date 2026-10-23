/** Contratos da API REST do backend (espelham os DTOs de `interfaces.rest.dto`). */

export type RequestStatus = 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'FAILED';
export type RequestType = 'PURCHASE' | 'VACATION' | 'ACCESS' | 'PARALLEL';
export type ProcessKey = 'purchaseApproval' | 'vacationApproval' | 'accessRequest' | 'parallelApproval';
export type TaskAction = 'APPROVE' | 'REJECT' | 'COMPLETE';
export type TaskStatus = 'PENDING' | 'COMPLETED' | 'CANCELED';
export type ActivityStatus = 'ACTIVE' | 'COMPLETED' | 'FAILED';
export type AccessLevel = 'LEITURA' | 'ESCRITA' | 'ADMINISTRADOR';
export type DemoUserId = 'colaborador' | 'gestor' | 'ti';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DemoUser {
  id: DemoUserId;
  displayName: string;
  group: string;
}

export interface ProcessDefinition {
  id: string;
  key: ProcessKey;
  name: string;
  description: string | null;
  version: number;
  deploymentId: string;
  resourceName: string;
  suspended: boolean;
}

export interface ProcessInstance {
  id: string;
  businessKey: string | null;
  name: string | null;
  processDefinitionId: string;
  processDefinitionKey: ProcessKey;
  processDefinitionName: string;
  processDefinitionVersion: number;
  startUserId: string | null;
  startTime: string;
  endTime: string | null;
  durationMillis: number | null;
  endActivityId: string | null;
  status: 'RUNNING' | 'COMPLETED' | 'CANCELED';
}

export interface WorkflowRequest {
  id: string;
  number: string;
  type: RequestType;
  typeLabel: string;
  processKey: ProcessKey;
  requester: string;
  summary: string;
  amount: number | null;
  status: RequestStatus;
  statusDetail: string | null;
  processInstanceId: string | null;
  processDefinitionId: string | null;
  openedAt: string;
  finishedAt: string | null;
  demo: boolean;
  currentSteps: string[];
}

export interface WorkTask {
  id: string;
  name: string;
  taskDefinitionKey: string;
  processInstanceId: string;
  processDefinitionId: string;
  processDefinitionKey: ProcessKey;
  processDefinitionName: string;
  businessKey: string | null;
  assignee: string | null;
  candidateGroups: string[];
  createdAt: string;
  endedAt: string | null;
  durationMillis: number | null;
  status: TaskStatus;
  decision: TaskAction | null;
  comment: string | null;
  allowedActions: TaskAction[];
  decisionTask: boolean;
  variables: Record<string, unknown>;
}

export interface Activity {
  id: string;
  activityId: string;
  name: string | null;
  type: string;
  executionId: string;
  taskId: string | null;
  assignee: string | null;
  startTime: string;
  endTime: string | null;
  durationMillis: number | null;
  status: ActivityStatus;
  decision: TaskAction | null;
  comment: string | null;
}

export interface ElementState {
  elementId: string;
  status: ActivityStatus;
  executionCount: number;
  failureMessage: string | null;
}

export interface DiagramState {
  processInstanceId: string;
  processDefinitionId: string;
  ended: boolean;
  executions: Activity[];
  elements: ElementState[];
}

export interface ProcessInstanceDetails {
  instance: ProcessInstance;
  request: WorkflowRequest | null;
  activeTasks: WorkTask[];
  failures: { elementId: string; message: string | null }[];
}

export interface ProcessVariable {
  name: string;
  type: string;
  value: unknown;
  lastUpdated: string | null;
}

export interface DashboardSummary {
  started: number;
  running: number;
  completed: number;
  pendingTasks: number;
  approved: number;
  rejected: number;
  failed: number;
}

export interface StatusCount {
  status: RequestStatus;
  count: number;
}

export interface ProcessTypeCount {
  processDefinitionKey: ProcessKey;
  name: string;
  running: number;
  completed: number;
  total: number;
}

export interface StartProcessResponse {
  request: WorkflowRequest;
  activeTasks: WorkTask[];
}

export interface TaskCompletion {
  taskId: string;
  processInstanceId: string;
  processEnded: boolean;
  nextTasks: WorkTask[];
}

export type StartProcessRequest =
  | { processKey: 'purchaseApproval'; requester: string; description: string; amount: number; justification?: string }
  | { processKey: 'vacationApproval'; requester: string; startDate: string; endDate: string; notes?: string }
  | { processKey: 'accessRequest'; requester: string; system: string; accessLevel: AccessLevel; justification: string }
  | { processKey: 'parallelApproval'; requester: string; title: string; description: string; estimatedAmount: number };

/** Corpo RFC 9457 retornado pelo backend em caso de erro. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
}
