import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AccessLevel, StartProcessRequest, StartProcessResponse } from '../../core/api/api.models';
import { WorkflowApi } from '../../core/api/workflow-api.service';
import { ProcessMeta } from '../../core/catalog';
import { problemMessage } from '../../core/http/problem';

/** Converte Date do datepicker para ISO local (yyyy-MM-dd), sem deslocamento de fuso. */
export function toIsoDate(date: Date | null): string {
  if (!date) {
    return '';
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Formulário único com os campos de todos os processos; apenas os do processo escolhido ficam ativos. */
export function buildStartForm(key: ProcessMeta['key']) {
  const form = new FormGroup({
    requester: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(1000)] }),
    amount: new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]),
    justification: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    startDate: new FormControl<Date | null>(null, Validators.required),
    endDate: new FormControl<Date | null>(null, Validators.required),
    notes: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    system: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    accessLevel: new FormControl<AccessLevel | null>(null, Validators.required),
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
  });
  const fields: Record<ProcessMeta['key'], string[]> = {
    purchaseApproval: ['requester', 'description', 'amount', 'justification'],
    vacationApproval: ['requester', 'startDate', 'endDate', 'notes'],
    accessRequest: ['requester', 'system', 'accessLevel', 'justification'],
    parallelApproval: ['requester', 'title', 'description', 'amount'],
  };
  Object.keys(form.controls).forEach((name) => {
    if (!fields[key].includes(name)) {
      form.get(name)!.disable();
    }
  });
  if (key === 'accessRequest') {
    form.controls.justification.addValidators(Validators.required);
    form.controls.justification.updateValueAndValidity();
  }
  return form;
}

export type StartForm = ReturnType<typeof buildStartForm>;

export function toStartRequest(key: ProcessMeta['key'], form: StartForm): StartProcessRequest {
  const v = form.getRawValue();
  switch (key) {
    case 'purchaseApproval':
      return { processKey: key, requester: v.requester, description: v.description, amount: v.amount!, justification: v.justification };
    case 'vacationApproval':
      return { processKey: key, requester: v.requester, startDate: toIsoDate(v.startDate), endDate: toIsoDate(v.endDate), notes: v.notes };
    case 'accessRequest':
      return { processKey: key, requester: v.requester, system: v.system, accessLevel: v.accessLevel!, justification: v.justification };
    case 'parallelApproval':
      return { processKey: key, requester: v.requester, title: v.title, description: v.description, estimatedAmount: v.amount! };
  }
}

@Component({
  selector: 'fl-start-process-dialog',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, MatIconModule, MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './start-process.dialog.html',
  styleUrl: './start-process.dialog.scss',
})
export class StartProcessDialog {
  protected readonly meta = inject<ProcessMeta>(MAT_DIALOG_DATA);
  private readonly api = inject(WorkflowApi);
  private readonly dialogRef = inject(MatDialogRef<StartProcessDialog, StartProcessResponse>);

  protected readonly form = buildStartForm(this.meta.key);
  protected readonly submitting = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly accessLevels: { value: AccessLevel; label: string }[] = [
    { value: 'LEITURA', label: 'Leitura' },
    { value: 'ESCRITA', label: 'Escrita' },
    { value: 'ADMINISTRADOR', label: 'Administrador' },
  ];
  protected readonly today = new Date();

  isActive(name: string): boolean {
    return this.form.get(name)!.enabled;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.serverError.set(null);
    this.api.startProcess(toStartRequest(this.meta.key, this.form)).subscribe({
      next: (response) => this.dialogRef.close(response),
      error: (error) => {
        this.serverError.set(problemMessage(error));
        this.submitting.set(false);
      },
    });
  }
}
