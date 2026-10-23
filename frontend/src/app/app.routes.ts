import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Dashboard · FlowableLab',
    loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
  },
  {
    path: 'processes',
    title: 'Processos · FlowableLab',
    loadComponent: () => import('./features/processes/catalog.page').then((m) => m.CatalogPage),
  },
  {
    path: 'requests',
    title: 'Solicitações · FlowableLab',
    loadComponent: () => import('./features/requests/requests.page').then((m) => m.RequestsPage),
  },
  {
    path: 'tasks',
    title: 'Tarefas · FlowableLab',
    loadComponent: () => import('./features/tasks/tasks.page').then((m) => m.TasksPage),
  },
  {
    path: 'viewer',
    title: 'Visualizador BPMN · FlowableLab',
    loadComponent: () => import('./features/processes/viewer.page').then((m) => m.ViewerPage),
  },
  {
    path: 'history',
    title: 'Histórico · FlowableLab',
    loadComponent: () => import('./features/history/history.page').then((m) => m.HistoryPage),
  },
  {
    path: 'instances/:id',
    title: 'Detalhes do processo · FlowableLab',
    loadComponent: () => import('./features/processes/process-detail.page').then((m) => m.ProcessDetailPage),
  },
  { path: '**', redirectTo: '' },
];
