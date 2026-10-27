import { Component, computed, inject, input } from '@angular/core';
import { Button } from 'primeng/button';
import { ThemeService } from '../../../core/theme/theme.service';

@Component({
  imports: [Button],
  selector: 'theme-toggle',
  styleUrl: './theme-toggle.component.scss',
  templateUrl: './theme-toggle.component.html',
})
export class ThemeToggleComponent {
  private themeService = inject(ThemeService);

  public compact = input(false);
  public label = computed(() => (this.themeService.isDark() ? 'Világos mód' : 'Sötét mód'));
  public icon = computed(() => (this.themeService.isDark() ? 'pi pi-sun' : 'pi pi-moon'));

  public onToggle(): void {
    this.themeService.toggle();
  }
}
