import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { PROCESS_CATALOG, ProcessMeta } from '../../core/catalog';
import { RefreshStore } from '../../core/state/refresh.store';
import { NotifyService } from '../../core/notify.service';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { StartProcessResponse } from '../../core/api/api.models';
import { StartProcessDialog } from './start-process.dialog';

@Component({
  selector: 'fl-catalog-page',
  imports: [RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './catalog.page.html',
  styleUrl: './catalog.page.scss',
})
export class CatalogPage {
  private readonly api = inject(WorkflowApi);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly refresh = inject(RefreshStore);
  private readonly notify = inject(NotifyService);

  /** Definições realmente implantadas no Flowable (versão e id exibidos em cada card). */
  private readonly definitions = rxResource({ stream: () => this.api.definitions() });
  private readonly stats = rxResource({
    params: () => this.refresh.version(),
    stream: () => this.api.typeDistribution(),
  });

  protected readonly cards = computed(() =>
    PROCESS_CATALOG.map((meta) => ({
      meta,
      definition: this.definitions.value()?.find((d) => d.key === meta.key) ?? null,
      stats: this.stats.value()?.find((s) => s.processDefinitionKey === meta.key) ?? null,
    })),
  );

  start(meta: ProcessMeta): void {
    this.dialog
      .open<StartProcessDialog, ProcessMeta, StartProcessResponse>(StartProcessDialog, {
        data: meta,
        width: '600px',
        maxWidth: '95vw',
        autoFocus: 'first-tabbable',
      })
      .afterClosed()
      .subscribe((response) => {
        if (!response) {
          return;
        }
        this.refresh.bump();
        this.notify.success(`Solicitação ${response.request.number} criada — instância iniciada no Flowable.`);
        this.router.navigate(['/instances', response.request.processInstanceId]);
      });
  }
}
