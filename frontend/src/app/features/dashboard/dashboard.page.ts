import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ChartConfiguration } from 'chart.js';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { RefreshStore } from '../../core/state/refresh.store';
import { DemoUserStore } from '../../core/state/demo-user.store';
import { PROCESS_CATALOG, STATUS_LABELS, processMeta } from '../../core/catalog';
import { KpiCardComponent } from '../../shared/kpi-card.component';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { ChartComponent } from '../../shared/chart.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { problemMessage } from '../../core/http/problem';

const STATUS_COLORS: Record<string, string> = {
  IN_PROGRESS: '#2f6bff',
  APPROVED: '#16a34a',
  REJECTED: '#e11d48',
  FAILED: '#f59e0b',
};

@Component({
  selector: 'fl-dashboard-page',
  imports: [
    RouterLink, DatePipe, CurrencyPipe, MatButtonModule, MatIconModule, MatProgressBarModule,
    KpiCardComponent, PageHeaderComponent, ChartComponent, StatusChipComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage {
  private readonly api = inject(WorkflowApi);
  private readonly refresh = inject(RefreshStore);
  protected readonly users = inject(DemoUserStore);
  protected readonly processMeta = processMeta;

  protected readonly summary = rxResource({
    params: () => this.refresh.version(),
    stream: () => this.api.summary(),
  });
  private readonly statuses = rxResource({
    params: () => this.refresh.version(),
    stream: () => this.api.statusDistribution(),
  });
  private readonly types = rxResource({
    params: () => this.refresh.version(),
    stream: () => this.api.typeDistribution(),
  });
  protected readonly recent = rxResource({
    params: () => this.refresh.version(),
    stream: () => this.api.requests({ size: 6 }),
  });

  protected readonly loading = computed(
    () => this.summary.isLoading() || this.statuses.isLoading() || this.types.isLoading(),
  );
  protected readonly error = computed(() => {
    const err = this.summary.error() ?? this.statuses.error() ?? this.types.error();
    return err ? problemMessage(err) : null;
  });

  protected readonly statusChart = computed<ChartConfiguration | null>(() => {
    if (!this.statuses.hasValue()) {
      return null;
    }
    const data = this.statuses.value();
    return {
      type: 'doughnut',
      data: {
        labels: data.map((d) => STATUS_LABELS[d.status]),
        datasets: [{
          data: data.map((d) => d.count),
          backgroundColor: data.map((d) => STATUS_COLORS[d.status]),
          borderWidth: 0,
        }],
      },
      options: {
        cutout: '68%',
        plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8 } } },
      },
    };
  });

  protected readonly typeChart = computed<ChartConfiguration | null>(() => {
    if (!this.types.hasValue()) {
      return null;
    }
    const ordered = PROCESS_CATALOG.map((meta) => ({
      meta,
      stats: this.types.value()?.find((t) => t.processDefinitionKey === meta.key),
    }));
    return {
      type: 'bar',
      data: {
        labels: ordered.map((o) => o.meta.title),
        datasets: [
          {
            label: 'Em andamento',
            data: ordered.map((o) => o.stats?.running ?? 0),
            backgroundColor: '#2f6bff',
            borderRadius: 6,
          },
          {
            label: 'Concluídos',
            data: ordered.map((o) => o.stats?.completed ?? 0),
            backgroundColor: '#16a34a',
            borderRadius: 6,
          },
        ],
      },
      options: {
        scales: {
          x: { stacked: true, grid: { display: false } },
          y: { stacked: true, beginAtZero: true, ticks: { precision: 0 } },
        },
        plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8 } } },
      },
    };
  });

  reload(): void {
    this.refresh.bump();
  }
}
