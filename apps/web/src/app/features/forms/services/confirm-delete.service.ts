import { Service, inject } from '@angular/core';
import { ConfirmationService } from 'primeng/api';

@Service()
export class ConfirmDeleteService {
  private confirmation = inject(ConfirmationService);

  public confirm(onAccept: () => void): void {
    this.confirmation.confirm({
      header: 'Piszkozat törlése',
      message: 'Biztosan törlöd?',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Törlés',
      rejectLabel: 'Mégse',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      defaultFocus: 'reject',
      closeOnEscape: true,
      accept: onAccept,
    });
  }
}
