import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Activity, ProcessInstance } from '../core/api/api.models';
import { ACTION_LABELS, ACTIVITY_TYPE_LABELS } from '../core/catalog';
import { DurationPipe } from './duration.pipe';

const ICONS: Record<string, string> = {
  startEvent: 'play_circle',
  endEvent: 'stop_circle',
  userTask: 'person',
  serviceTask: 'settings',
  exclusiveGateway: 'alt_route',
  parallelGateway: 'call_split',
};

/** Timeline do histórico real. Clicar em um item seleciona o elemento no diagrama BPMN. */
@Component({
  selector: 'fl-history-timeline',
  imports: [DatePipe, DurationPipe, MatIconModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './history-timeline.component.html',
  styleUrl: './history-timeline.component.scss',
})
export class HistoryTimelineComponent {
  readonly activities = input<Activity[]>([]);
  readonly instance = input<ProcessInstance | null>();
  readonly outcome = input<string | null>();
  readonly selectedId = model<string | null>(null);

  protected readonly icons = ICONS;
  protected readonly typeLabels = ACTIVITY_TYPE_LABELS;
  protected readonly actionLabels = ACTION_LABELS;

  protected readonly items = computed(() => this.activities());

  select(activity: Activity): void {
    this.selectedId.set(this.selectedId() === activity.activityId ? null : activity.activityId);
  }
}
