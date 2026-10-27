import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialog } from 'primeng/confirmdialog';

export async function chooseOption(fixture: ComponentFixture<unknown>, selector: string, label: string): Promise<void> {
  const root = fixture.nativeElement as HTMLElement;
  (root.querySelector(selector) as HTMLElement).click();
  await fixture.whenStable();
  const options = Array.from(document.querySelectorAll('[role="option"]')) as HTMLElement[];
  const option = options.find((element) => element.textContent?.trim() === label);
  if (!option) {
    throw new Error(`No option "${label}" in ${selector}`);
  }
  option.click();
  await fixture.whenStable();
}

export function renderConfirmDialog(): ComponentFixture<ConfirmDialog> {
  return TestBed.createComponent(ConfirmDialog);
}

export async function confirmDialogButton(
  dialog: ComponentFixture<ConfirmDialog>,
  label: string,
): Promise<HTMLButtonElement> {
  await dialog.whenStable();
  const buttons = Array.from(document.querySelectorAll('.p-confirmdialog button')) as HTMLButtonElement[];
  const button = buttons.find((element) => element.textContent?.trim() === label);
  if (!button) {
    throw new Error(`No confirm dialog button "${label}"`);
  }
  return button;
}

export function confirmDialogMessage(): string {
  return document.querySelector('.p-confirmdialog')?.textContent ?? '';
}
