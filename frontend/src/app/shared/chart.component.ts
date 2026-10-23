import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';

Chart.register(DoughnutController, ArcElement, BarController, BarElement, CategoryScale, LinearScale, Legend, Tooltip);

/** Wrapper mínimo do Chart.js: recria o gráfico quando a configuração (signal) muda. */
@Component({
  selector: 'fl-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<div class="wrap" [style.height]="height()"><canvas #canvas></canvas></div>',
  styles: '.wrap { position: relative; width: 100%; }',
})
export class ChartComponent {
  readonly config = input.required<ChartConfiguration>();
  readonly height = input('260px');

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart: Chart | null = null;
  private rendered = false;

  constructor() {
    afterNextRender(() => {
      this.rendered = true;
      this.render(this.config());
    });
    effect(() => {
      const config = this.config();
      if (this.rendered) {
        this.render(config);
      }
    });
    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  private render(config: ChartConfiguration): void {
    this.chart?.destroy();
    this.chart = new Chart(this.canvas().nativeElement, {
      ...config,
      options: { responsive: true, maintainAspectRatio: false, ...config.options },
    });
  }
}
