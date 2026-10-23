import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { DEMO_PROFILES, DemoUserStore } from '../core/state/demo-user.store';
import { RefreshStore } from '../core/state/refresh.store';
import { WorkflowApi } from '../core/api/workflow-api.service';
import { DemoUserId } from '../core/api/api.models';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  exact?: boolean;
}

@Component({
  selector: 'fl-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, DatePipe,
    MatSidenavModule, MatToolbarModule, MatListModule, MatIconModule, MatButtonModule, MatMenuModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  protected readonly users = inject(DemoUserStore);
  protected readonly refresh = inject(RefreshStore);
  private readonly api = inject(WorkflowApi);

  protected readonly profiles = DEMO_PROFILES;
  protected readonly isMobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 960px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  /** Quantidade de tarefas pendentes do perfil atual (badge do menu). */
  private readonly myTasks = rxResource({
    params: () => ({ user: this.users.userId(), v: this.refresh.version() }),
    stream: ({ params }) => this.api.tasks({ assignee: params.user, size: 1 }),
  });
  protected readonly pendingCount = computed(() =>
    this.myTasks.hasValue() ? this.myTasks.value().totalElements : null,
  );

  protected readonly nav: NavItem[] = [
    { path: '/', label: 'Dashboard', icon: 'space_dashboard', exact: true },
    { path: '/processes', label: 'Processos', icon: 'account_tree' },
    { path: '/requests', label: 'Solicitações', icon: 'list_alt' },
    { path: '/tasks', label: 'Tarefas', icon: 'task_alt' },
    { path: '/viewer', label: 'Visualizador BPMN', icon: 'schema' },
    { path: '/history', label: 'Histórico', icon: 'history' },
  ];

  selectUser(id: DemoUserId): void {
    this.users.select(id);
  }
}
