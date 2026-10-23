import { ActivityStatus, ProcessKey, RequestStatus, RequestType, TaskAction } from './api/api.models';

/** Metadados apenas visuais (ícones, cores, textos). As regras de negócio ficam no backend. */
export interface ProcessMeta {
  key: ProcessKey;
  type: RequestType;
  title: string;
  icon: string;
  color: string;
  soft: string;
  description: string;
  features: string[];
}

export const PROCESS_CATALOG: readonly ProcessMeta[] = [
  {
    key: 'purchaseApproval',
    type: 'PURCHASE',
    title: 'Aprovação de compra',
    icon: 'shopping_cart',
    color: '#6d4aff',
    soft: '#efeaff',
    description: 'Validação automática do pedido, análise do gestor e decisão por gateway exclusivo.',
    features: ['Service Task', 'User Task', 'Exclusive Gateway'],
  },
  {
    key: 'vacationApproval',
    type: 'VACATION',
    title: 'Aprovação de férias',
    icon: 'beach_access',
    color: '#0ea5e9',
    soft: '#e0f4fd',
    description: 'Análise do período pelo gestor e registro assíncrono no RH local quando aprovado.',
    features: ['Async Service Task', 'Retentativas', 'Dead letter'],
  },
  {
    key: 'accessRequest',
    type: 'ACCESS',
    title: 'Solicitação de acesso',
    icon: 'vpn_key',
    color: '#f59e0b',
    soft: '#fff3df',
    description: 'Aprovação do gestor seguida do provisionamento pela equipe de TI.',
    features: ['Múltiplas User Tasks', 'Equipes diferentes'],
  },
  {
    key: 'parallelApproval',
    type: 'PARALLEL',
    title: 'Aprovação paralela',
    icon: 'call_split',
    color: '#10b981',
    soft: '#e3f6ea',
    description: 'Análises técnica e financeira simultâneas; avança somente quando ambas terminam.',
    features: ['Parallel Gateway', 'Fork / Join'],
  },
];

export function processMeta(key: string | null | undefined): ProcessMeta {
  return PROCESS_CATALOG.find((p) => p.key === key || p.type === key) ?? PROCESS_CATALOG[0];
}

export const STATUS_LABELS: Record<RequestStatus, string> = {
  IN_PROGRESS: 'Em andamento',
  APPROVED: 'Aprovada',
  REJECTED: 'Rejeitada',
  FAILED: 'Falha',
};

export const ACTION_LABELS: Record<TaskAction, string> = {
  APPROVE: 'Aprovar',
  REJECT: 'Rejeitar',
  COMPLETE: 'Concluir',
};

export const ACTIVITY_STATUS_LABELS: Record<ActivityStatus, string> = {
  ACTIVE: 'Em execução',
  COMPLETED: 'Concluída',
  FAILED: 'Falhou',
};

export const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  startEvent: 'Evento de início',
  endEvent: 'Evento de fim',
  userTask: 'User Task',
  serviceTask: 'Service Task',
  exclusiveGateway: 'Exclusive Gateway',
  parallelGateway: 'Parallel Gateway',
  sequenceFlow: 'Sequence Flow',
};

export const VARIABLE_LABELS: Record<string, string> = {
  requester: 'Solicitante',
  description: 'Descrição',
  amount: 'Valor',
  justification: 'Justificativa',
  startDate: 'Data inicial',
  endDate: 'Data final',
  days: 'Dias',
  notes: 'Observações',
  system: 'Sistema',
  accessLevel: 'Nível de acesso',
  title: 'Título',
  estimatedAmount: 'Valor estimado',
  priority: 'Prioridade',
};
