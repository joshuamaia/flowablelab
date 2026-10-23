import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { StatusChipComponent } from './status-chip.component';
import { KpiCardComponent } from './kpi-card.component';
import { TaskCardComponent } from './task-card.component';
import { WorkTask } from '../core/api/api.models';

describe('Componentes compartilhados', () => {
  it('StatusChip traduz o status e aplica o tom visual', () => {
    const fixture = TestBed.createComponent(StatusChipComponent);
    fixture.componentRef.setInput('status', 'REJECTED');
    fixture.detectChanges();
    const chip: HTMLElement = fixture.nativeElement.querySelector('.chip');
    expect(chip.textContent).toContain('Rejeitada');
    expect(chip.classList).toContain('danger');
  });

  it('KpiCard exibe rótulo e valor', () => {
    const fixture = TestBed.createComponent(KpiCardComponent);
    fixture.componentRef.setInput('label', 'Tarefas pendentes');
    fixture.componentRef.setInput('value', 7);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Tarefas pendentes');
    expect(fixture.nativeElement.querySelector('.value').textContent).toContain('7');
  });

  it('TaskCard mostra somente as ações permitidas e emite a escolhida', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const task: WorkTask = {
      id: 't1', name: 'Análise do gestor', taskDefinitionKey: 'managerReview', processInstanceId: 'p1',
      processDefinitionId: 'purchaseApproval:1:x', processDefinitionKey: 'purchaseApproval',
      processDefinitionName: 'Aprovação de compra', businessKey: 'REQ-1', assignee: null,
      candidateGroups: ['gestor'], createdAt: '2026-01-01T10:00:00Z', endedAt: null, durationMillis: null,
      status: 'PENDING', decision: null, comment: null, allowedActions: ['APPROVE', 'REJECT'], decisionTask: true,
      variables: { requester: 'Ana', amount: 1500 },
    };
    const fixture = TestBed.createComponent(TaskCardComponent);
    fixture.componentRef.setInput('task', task);
    fixture.detectChanges();

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button.act'));
    expect(buttons.map((b) => b.textContent?.trim())).toEqual(['checkAprovar', 'closeRejeitar']);
    expect(fixture.nativeElement.textContent).toContain('Ana');

    let emitted: string | undefined;
    fixture.componentInstance.action.subscribe((a) => (emitted = a));
    buttons[1].click();
    expect(emitted).toBe('REJECT');
  });
});
