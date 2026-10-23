import { Injectable, computed, signal } from '@angular/core';
import { DemoUserId } from '../api/api.models';

const STORAGE_KEY = 'flowablelab.demoUser';

export interface DemoProfile {
  id: DemoUserId;
  name: string;
  role: string;
  icon: string;
}

/** Perfis demonstrativos (espelham GET /api/users). Não há autenticação real. */
export const DEMO_PROFILES: readonly DemoProfile[] = [
  { id: 'colaborador', name: 'Carla Colaboradora', role: 'Colaborador', icon: 'person' },
  { id: 'gestor', name: 'Gabriel Gestor', role: 'Gestor', icon: 'supervisor_account' },
  { id: 'ti', name: 'Tiago da TI', role: 'Equipe de TI', icon: 'engineering' },
];

function readStored(): DemoUserId {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return DEMO_PROFILES.some((p) => p.id === stored) ? (stored as DemoUserId) : 'gestor';
  } catch {
    return 'gestor';
  }
}

/** Perfil selecionado no cabeçalho, enviado ao backend no header X-Demo-User. */
@Injectable({ providedIn: 'root' })
export class DemoUserStore {
  private readonly current = signal<DemoUserId>(readStored());

  readonly userId = this.current.asReadonly();
  readonly profile = computed(() => DEMO_PROFILES.find((p) => p.id === this.current())!);

  select(id: DemoUserId): void {
    this.current.set(id);
    try {
      localStorage.setItem(STORAGE_KEY, id);
    } catch {
      // armazenamento indisponível: o perfil vale apenas para esta sessão
    }
  }
}
