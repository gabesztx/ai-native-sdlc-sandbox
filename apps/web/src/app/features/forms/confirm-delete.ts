import { ConfirmationService } from 'primeng/api';

export function confirmDelete(confirmation: ConfirmationService, onAccept: () => void): void {
  confirmation.confirm({
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
