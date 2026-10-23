import { TestBed } from '@angular/core/testing';
import { computed } from '@angular/core';
import { DemoUserStore } from './demo-user.store';
import { RefreshStore } from './refresh.store';

describe('Stores (Signals)', () => {
  beforeEach(() => localStorage.clear());

  it('troca o perfil demonstrativo, atualiza sinais derivados e persiste a escolha', () => {
    const store = TestBed.inject(DemoUserStore);
    const label = computed(() => store.profile().role);
    expect(store.userId()).toBe('gestor');

    store.select('ti');
    expect(store.userId()).toBe('ti');
    expect(label()).toBe('Equipe de TI');
    expect(localStorage.getItem('flowablelab.demoUser')).toBe('ti');
  });

  it('incrementa a versão global de atualização', () => {
    const refresh = TestBed.inject(RefreshStore);
    const before = refresh.version();
    refresh.bump();
    refresh.bump();
    expect(refresh.version()).toBe(before + 2);
  });
});
