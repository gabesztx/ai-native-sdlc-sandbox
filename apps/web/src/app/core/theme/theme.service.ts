import { DOCUMENT, Service, computed, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'hugo-theme';
const DARK_CLASS = 'app-dark';

@Service()
export class ThemeService {
  private document = inject(DOCUMENT);

  public theme = signal<ThemeMode>(readStoredTheme());
  public isDark = computed(() => this.theme() === 'dark');

  public init(): void {
    this.apply(this.theme());
  }

  public toggle(): void {
    const next: ThemeMode = this.isDark() ? 'light' : 'dark';
    this.theme.set(next);
    this.apply(next);
    writeStoredTheme(next);
  }

  private apply(theme: ThemeMode): void {
    this.document.documentElement.classList.toggle(DARK_CLASS, theme === 'dark');
  }
}

function readStoredTheme(): ThemeMode {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

function writeStoredTheme(theme: ThemeMode): void {
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    return;
  }
}
