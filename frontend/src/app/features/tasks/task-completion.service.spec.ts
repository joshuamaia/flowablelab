import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotifyService } from '../../core/notify.service';
import { RefreshStore } from '../../core/state/refresh.store';
import { WorkTask } from '../../core/api/api.models';
import { TaskCompletionService } from './task-completion.service';

const task = { id: 't1', name: 'Análise do gestor' } as WorkTask;

describe('TaskCompletionService', () => {
  let service: TaskCompletionService;
  let http: HttpTestingController;
  let refresh: RefreshStore;
  const notify = { success: vi.fn(), error: vi.fn() };

  beforeEach(() => {
    notify.success.mockReset();
    notify.error.mockReset();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: NotifyService, useValue: notify }],
    });
    service = TestBed.inject(TaskCompletionService);
    http = TestBed.inject(HttpTestingController);
    refresh = TestBed.inject(RefreshStore);
  });

  it('após concluir, atualiza o estado global (signals) e informa a próxima etapa', () => {
    const version = refresh.version();
    service.complete(task, 'APPROVE', 'ok').subscribe();
    expect(service.busyTaskId()).toBe('t1');

    http.expectOne('/api/tasks/t1/complete').flush({
      taskId: 't1', processInstanceId: 'p1', processEnded: false, nextTasks: [{ name: 'Tarefa TI' }],
    });

    expect(refresh.version()).toBe(version + 1);
    expect(service.busyTaskId()).toBeNull();
    expect(notify.success).toHaveBeenCalledWith(expect.stringContaining('Próxima etapa: Tarefa TI'));
  });

  it('em caso de erro notifica e também recarrega o estado', () => {
    const version = refresh.version();
    service.complete(task, 'APPROVE', null).subscribe({ error: () => undefined });
    http.expectOne('/api/tasks/t1/complete').flush(
      { detail: 'A tarefa já foi concluída.' }, { status: 409, statusText: 'Conflict' });

    expect(notify.error).toHaveBeenCalled();
    expect(refresh.version()).toBe(version + 1);
    expect(service.busyTaskId()).toBeNull();
  });
});
