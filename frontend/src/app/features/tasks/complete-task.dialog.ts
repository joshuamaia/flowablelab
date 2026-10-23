import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { TaskAction, WorkTask } from '../../core/api/api.models';
import { ACTION_LABELS } from '../../core/catalog';

export interface CompleteTaskDialogData {
  task: WorkTask;
  action: TaskAction;
}

export interface CompleteTaskDialogResult {
  comment: string;
}

@Component({
  selector: 'fl-complete-task-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>
      <mat-icon [class]="data.action.toLowerCase()">{{ icon() }}</mat-icon>
      {{ labels[data.action] }} · {{ data.task.name }}
    </h2>
    <mat-dialog-content>
      <p class="fl-muted">
        {{ data.task.processDefinitionName }} · <span class="fl-mono">{{ data.task.businessKey }}</span>
      </p>
      <mat-form-field appearance="outline">
        <mat-label>{{ data.action === 'REJECT' ? 'Justificativa (obrigatória)' : 'Comentário (opcional)' }}</mat-label>
        <textarea matInput [formControl]="comment" rows="3" maxlength="1000" cdkFocusInitial></textarea>
        @if (comment.hasError('required')) {
          <mat-error>A rejeição exige uma justificativa.</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button [class]="data.action.toLowerCase()" (click)="confirm()">{{ labels[data.action] }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    h2 { display: flex; align-items: center; gap: 8px; }
    mat-form-field { width: 100%; }
    mat-dialog-content { min-width: min(460px, 80vw); }
    mat-icon.approve { color: var(--fl-completed); }
    mat-icon.reject { color: var(--fl-failed); }
    button.reject { --mat-button-filled-container-color: var(--fl-failed); }
    button.approve { --mat-button-filled-container-color: var(--fl-completed); }
  `,
})
export class CompleteTaskDialog {
  protected readonly data = inject<CompleteTaskDialogData>(MAT_DIALOG_DATA);
  protected readonly labels = ACTION_LABELS;
  protected readonly comment = new FormControl('', {
    nonNullable: true,
    validators: this.data.action === 'REJECT' ? [Validators.required, Validators.maxLength(1000)] : [Validators.maxLength(1000)],
  });
  protected readonly icon = computed(() =>
    this.data.action === 'APPROVE' ? 'check_circle' : this.data.action === 'REJECT' ? 'cancel' : 'task_alt',
  );
  private readonly ref = inject<MatDialogRef<CompleteTaskDialog, CompleteTaskDialogResult>>(MatDialogRef);

  confirm(): void {
    if (this.comment.invalid) {
      this.comment.markAsTouched();
      return;
    }
    this.ref.close({ comment: this.comment.value.trim() } satisfies CompleteTaskDialogResult);
  }
}
