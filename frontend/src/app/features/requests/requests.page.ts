import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { RefreshStore } from '../../core/state/refresh.store';
import { RequestStatus, RequestType, WorkflowRequest } from '../../core/api/api.models';
import { PROCESS_CATALOG, STATUS_LABELS, processMeta } from '../../core/catalog';
import { problemMessage } from '../../core/http/problem';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

@Component({
  selector: 'fl-requests-page',
  imports: [
    DatePipe, CurrencyPipe, RouterLink, MatTableModule, MatPaginatorModule, MatSortModule, MatFormFieldModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatTooltipModule, MatProgressBarModule,
    PageHeaderComponent, StatusChipComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './requests.page.html',
  styleUrl: './requests.page.scss',
})
export class RequestsPage {
  private readonly api = inject(WorkflowApi);
  private readonly refresh = inject(RefreshStore);
  private readonly router = inject(Router);

  protected readonly processMeta = processMeta;
  protected readonly types = PROCESS_CATALOG;
  protected readonly statusOptions = Object.entries(STATUS_LABELS) as [RequestStatus, string][];
  protected readonly columns = ['number', 'type', 'requester', 'openedAt', 'status', 'step', 'actions'];

  protected readonly status = signal<RequestStatus | null>(null);
  protected readonly type = signal<RequestType | null>(null);
  protected readonly page = signal(0);
  protected readonly size = signal(10);
  protected readonly sort = signal<Sort>({ active: 'openedAt', direction: 'desc' });

  protected readonly requests = rxResource({
    params: () => ({
      status: this.status(),
      type: this.type(),
      page: this.page(),
      size: this.size(),
      sort: this.sort().active,
      direction: this.sort().direction || 'desc',
      v: this.refresh.version(),
    }),
    stream: ({ params }) => this.api.requests(params),
  });

  protected readonly rows = computed(() => this.requests.value()?.content ?? []);
  protected readonly total = computed(() => this.requests.value()?.totalElements ?? 0);
  protected readonly error = computed(() => (this.requests.error() ? problemMessage(this.requests.error()) : null));

  setStatus(value: RequestStatus | null): void {
    this.status.set(value);
    this.page.set(0);
  }

  setType(value: RequestType | null): void {
    this.type.set(value);
    this.page.set(0);
  }

  onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
  }

  onSort(sort: Sort): void {
    this.sort.set(sort.direction ? sort : { active: 'openedAt', direction: 'desc' });
    this.page.set(0);
  }

  open(row: WorkflowRequest): void {
    if (row.processInstanceId) {
      this.router.navigate(['/instances', row.processInstanceId]);
    }
  }
}
