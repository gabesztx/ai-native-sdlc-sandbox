import { EnvironmentProviders, Provider } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

export function provideUiTesting(): (Provider | EnvironmentProviders)[] {
  return [providePrimeNG(), MessageService, ConfirmationService];
}
