import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { PROCESS_CATALOG } from '../../core/catalog';
import { StartProcessDialog, buildStartForm, toIsoDate, toStartRequest } from './start-process.dialog';

describe('Formulário de início de processo', () => {
  it('habilita somente os campos do processo escolhido', () => {
    const form = buildStartForm('vacationApproval');
    expect(form.controls.startDate.enabled).toBe(true);
    expect(form.controls.amount.disabled).toBe(true);
    expect(form.controls.system.disabled).toBe(true);
  });

  it('valida campos obrigatórios e valor positivo', () => {
    const form = buildStartForm('purchaseApproval');
    expect(form.valid).toBe(false);
    form.patchValue({ requester: 'Ana', description: 'Notebook', amount: 0 });
    expect(form.controls.amount.hasError('min')).toBe(true);
    form.patchValue({ amount: 10 });
    expect(form.valid).toBe(true);
  });

  it('justificativa é obrigatória apenas na solicitação de acesso', () => {
    expect(buildStartForm('accessRequest').controls.justification.hasError('required')).toBe(true);
    expect(buildStartForm('purchaseApproval').controls.justification.hasError('required')).toBe(false);
  });

  it('converte datas para ISO local sem deslocar o dia', () => {
    expect(toIsoDate(new Date(2026, 11, 1, 23, 30))).toBe('2026-12-01');
    const form = buildStartForm('vacationApproval');
    form.patchValue({ requester: 'Bruno', startDate: new Date(2026, 11, 1), endDate: new Date(2026, 11, 10) });
    expect(toStartRequest('vacationApproval', form)).toEqual({
      processKey: 'vacationApproval', requester: 'Bruno', startDate: '2026-12-01', endDate: '2026-12-10', notes: '',
    });
  });

  it('envia ao backend e exibe o erro retornado em Problem Details', async () => {
    const close = vi.fn();
    TestBed.configureTestingModule({
      imports: [StartProcessDialog],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNativeDateAdapter(),
        { provide: MAT_DIALOG_DATA, useValue: PROCESS_CATALOG[0] },
        { provide: MatDialogRef, useValue: { close } },
      ],
    });
    const fixture = TestBed.createComponent(StartProcessDialog);
    const component = fixture.componentInstance as unknown as { form: ReturnType<typeof buildStartForm>; submit(): void };
    component.form.patchValue({ requester: 'Ana', description: 'Notebook', amount: 50 });
    component.submit();

    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/process-instances').flush(
      { detail: 'Regra violada no servidor' }, { status: 422, statusText: 'Unprocessable' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.server-error').textContent).toContain('Regra violada no servidor');
    expect(close).not.toHaveBeenCalled();
  });
});
