declare const PRIMEUI_LICENSE: string | undefined;

export const primeUiLicense =
  typeof PRIMEUI_LICENSE === 'string' && PRIMEUI_LICENSE !== '' ? PRIMEUI_LICENSE : undefined;
