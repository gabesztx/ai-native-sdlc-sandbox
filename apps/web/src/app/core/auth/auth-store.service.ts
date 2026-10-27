import { Service, computed, signal } from '@angular/core';
import { AuthTokens } from '../../api/models';
import { StoredAuth } from '../../shared/models/stored-auth.model';

const STORAGE_KEY = 'hugo.auth';

@Service()
export class AuthStoreService {
  private state = signal<StoredAuth | null>(readStoredAuth());

  public me = computed(() => this.state()?.me ?? null);
  public role = computed(() => this.state()?.me.role ?? null);
  public isLoggedIn = computed(() => this.state() !== null);
  public homePath = computed(() => homePathFor(this.role()));

  public accessToken(): string | null {
    return this.state()?.accessToken ?? null;
  }

  public refreshToken(): string | null {
    return this.state()?.refreshToken ?? null;
  }

  public save(tokens: AuthTokens): void {
    const auth: StoredAuth = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      me: tokens.me,
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    this.state.set(auth);
  }

  public clear(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.state.set(null);
  }
}

function readStoredAuth(): StoredAuth | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

function homePathFor(role: string | null): string {
  if (role === 'ADMIN') {
    return '/admin/forms';
  }
  if (role === 'MEMBER') {
    return '/reviews';
  }
  return '/login';
}
